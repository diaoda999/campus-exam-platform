package com.campus.exam.service.support;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 智能组卷求解器：知识点覆盖硬约束 + 多因子贪心 + 难度回溯。
 *
 * 求解步骤：
 * 1) 知识点配额优先：先为每个指定知识点选够最低题量（占用对应题型名额）；
 * 2) 贪心补额：剩余名额按 “难度贴合度 + 知识点稀缺度 + 使用次数均衡” 综合评分选题；
 * 3) 难度回溯：平均难度偏离目标超过容差时，做有限次定向换题，直到满足或达到回溯上限；
 * 4) 候选不足时返回未满足约束报告，而不是强行出一张不合格的卷。
 *
 * 纯函数、无 Spring 依赖，针对构造的小候选池即可单元测试。
 */
public final class PaperGenerator {

    /** 难度权重 */
    private static final double W_DIFFICULTY = 0.6;
    /** 知识点稀缺度权重 */
    private static final double W_KNOWLEDGE = 0.25;
    /** 使用次数均衡权重 */
    private static final double W_USE_COUNT = 0.15;

    private PaperGenerator() {
    }

    @Data
    @Builder
    public static class GroupReq {
        private Integer questionType;
        private Integer count;
    }

    @Data
    @Builder
    public static class GenResult {
        private List<Long> questionIds;
        private Double avgDifficulty;
        private boolean success;
        private List<String> unmetConstraints;
    }

    /**
     * @param pool         候选池（已按题型/状态过滤）
     * @param groups       各题型需求数量
     * @param targetDiff   期望平均难度，可空（为空则只做知识点+均衡）
     * @param tolerance    难度容差，默认 0.3
     * @param coverageMin  知识点最低题量，可空
     * @param swapLimit    难度回溯换题次数上限
     */
    public static GenResult generate(List<CandidateQuestion> pool,
                                     List<GroupReq> groups,
                                     Double targetDiff,
                                     Double tolerance,
                                     Map<Long, Integer> coverageMin,
                                     int swapLimit) {
        List<String> unmet = new ArrayList<>();
        Set<Long> chosen = new HashSet<>();
        Map<Integer, Integer> remainQuota = new HashMap<>();
        int totalCount = 0;
        for (GroupReq g : groups) {
            remainQuota.put(g.getQuestionType(), g.getCount());
            totalCount += g.getCount();
        }
        if (totalCount == 0) {
            return GenResult.builder().questionIds(List.of()).avgDifficulty(0d).success(true).unmetConstraints(unmet).build();
        }

        Map<Long, Integer> coverageRemain = coverageMin == null
                ? new HashMap<>() : new HashMap<>(coverageMin);

        // ---------- 第一步：知识点覆盖硬约束（贪心选最贴合难度的题） ----------
        for (Map.Entry<Long, Integer> entry : coverageRemain.entrySet()) {
            Long kpId = entry.getKey();
            int need = entry.getValue();
            List<CandidateQuestion> matched = pool.stream()
                    .filter(q -> q.getKnowledgeIds() != null && q.getKnowledgeIds().contains(kpId))
                    .filter(q -> !chosen.contains(q.getQuestionId()))
                    .filter(q -> remainQuota.getOrDefault(q.getType(), 0) > 0)
                    .sorted(comparatorFor(targetDiff))
                    .toList();
            int picked = 0;
            for (CandidateQuestion q : matched) {
                if (picked >= need) {
                    break;
                }
                chosen.add(q.getQuestionId());
                remainQuota.merge(q.getType(), -1, Integer::sum);
                picked++;
            }
            if (picked < need) {
                unmet.add(String.format("知识点[%d]要求至少%d题，候选池仅能满足%d题", kpId, need, picked));
            }
        }

        // ---------- 第二步：贪心补足各题型剩余名额 ----------
        for (Map.Entry<Integer, Integer> entry : remainQuota.entrySet()) {
            Integer type = entry.getKey();
            int need = entry.getValue();
            List<CandidateQuestion> typePool = pool.stream()
                    .filter(q -> type.equals(q.getType()))
                    .filter(q -> !chosen.contains(q.getQuestionId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            int picked = 0;
            while (picked < need) {
                CandidateQuestion best = pickBest(typePool, chosen, targetDiff, coverageRemain);
                if (best == null) {
                    unmet.add(String.format("题型[%d]要求%d题，候选池题量不足，缺%d题", type, need, need - picked));
                    break;
                }
                chosen.add(best.getQuestionId());
                picked++;
            }
        }

        // ---------- 第三步：难度回溯（有限次定向换题） ----------
        Map<Long, CandidateQuestion> byId = pool.stream()
                .collect(Collectors.toMap(CandidateQuestion::getQuestionId, q -> q, (a, b) -> a));
        if (targetDiff != null) {
            double tol = tolerance == null ? 0.3d : tolerance;
            int swaps = 0;
            while (swaps < swapLimit) {
                double avg = avgDifficulty(chosen, byId);
                double gap = avg - targetDiff;
                if (Math.abs(gap) <= tol || chosen.isEmpty()) {
                    break;
                }
                boolean swapped = false;
                // gap>0：当前偏难，尝试用更简单的未选题替换一道“最容易被替换”的已选题
                CandidateQuestion remove = chooseSwapOut(new ArrayList<>(chosen), byId, gap);
                if (remove != null) {
                    CandidateQuestion replace = chooseSwapIn(pool, chosen, remove, byId, targetDiff);
                    if (replace != null) {
                        chosen.remove(remove.getQuestionId());
                        chosen.add(replace.getQuestionId());
                        swaps++;
                        swapped = true;
                    }
                }
                if (!swapped) {
                    double finalAvg = avgDifficulty(chosen, byId);
                    if (Math.abs(finalAvg - targetDiff) > tol) {
                        unmet.add(String.format("期望平均难度%.2f，实际%.2f，已达换题上限无法进一步逼近", targetDiff, finalAvg));
                    }
                    break;
                }
            }
        }

        List<Long> ids = chosen.stream().sorted().toList();
        double avg = ids.isEmpty() ? 0d : avgDifficulty(chosen, byId);
        return GenResult.builder()
                .questionIds(ids)
                .avgDifficulty(round(avg))
                .success(unmet.isEmpty())
                .unmetConstraints(unmet)
                .build();
    }

    private static Comparator<CandidateQuestion> comparatorFor(Double targetDiff) {
        return Comparator.comparingDouble(q -> difficultyDistance(q, targetDiff));
    }

    /** 贪心评分：越小越优先 */
    private static CandidateQuestion pickBest(List<CandidateQuestion> typePool,
                                              Set<Long> chosen,
                                              Double targetDiff,
                                              Map<Long, Integer> coverageRemain) {
        CandidateQuestion best = null;
        double bestScore = Double.MAX_VALUE;
        for (CandidateQuestion q : typePool) {
            if (chosen.contains(q.getQuestionId())) {
                continue;
            }
            double diffScore = targetDiff == null ? 0 : W_DIFFICULTY * Math.abs(q.getDifficulty() - targetDiff) / 4d;
            // 命中仍缺配额的知识点越多越优先（稀缺度归一到 0~1）
            long hit = q.getKnowledgeIds() == null ? 0
                    : q.getKnowledgeIds().stream().filter(k -> coverageRemain.getOrDefault(k, 0) > 0).count();
            double knowScore = W_KNOWLEDGE * (1 - Math.min(hit, 2) / 2d);
            double useScore = W_USE_COUNT * Math.min(q.getUseCount() == null ? 0 : q.getUseCount(), 20) / 20d;
            double score = diffScore + knowScore + useScore;
            if (score < bestScore) {
                bestScore = score;
                best = q;
            }
        }
        return best;
    }

    private static double difficultyDistance(CandidateQuestion q, Double targetDiff) {
        if (targetDiff == null) {
            return 0;
        }
        return Math.abs(q.getDifficulty() - targetDiff);
    }

    /** gap>0 偏难：优先移除难题；gap<0 偏易：优先移除易题 */
    private static CandidateQuestion chooseSwapOut(List<Long> chosenIds,
                                                   Map<Long, CandidateQuestion> byId,
                                                   double gap) {
        return chosenIds.stream().map(byId::get).filter(java.util.Objects::nonNull)
                .max(Comparator.comparingInt(q -> gap > 0 ? q.getDifficulty() : -q.getDifficulty()))
                .orElse(null);
    }

    /** 找一道同题型、未选、换入后整体平均更接近目标的题 */
    private static CandidateQuestion chooseSwapIn(List<CandidateQuestion> pool,
                                                  Set<Long> chosen,
                                                  CandidateQuestion removed,
                                                  Map<Long, CandidateQuestion> byId,
                                                  double targetDiff) {
        double currentSum = chosen.stream().map(byId::get).filter(java.util.Objects::nonNull)
                .mapToInt(CandidateQuestion::getDifficulty).sum();
        int n = chosen.size();
        CandidateQuestion best = null;
        double bestGap = Double.MAX_VALUE;
        for (CandidateQuestion q : pool) {
            if (chosen.contains(q.getQuestionId()) || !q.getType().equals(removed.getType())) {
                continue;
            }
            double newAvg = (currentSum - removed.getDifficulty() + q.getDifficulty()) / (double) n;
            double gap = Math.abs(newAvg - targetDiff);
            if (gap < bestGap) {
                bestGap = gap;
                best = q;
            }
        }
        return bestGap < Math.abs((double) currentSum / n - targetDiff) ? best : null;
    }

    private static double avgDifficulty(Set<Long> chosen, Map<Long, CandidateQuestion> byId) {
        return chosen.stream().map(byId::get).filter(java.util.Objects::nonNull)
                .mapToInt(CandidateQuestion::getDifficulty).average().orElse(0d);
    }

    private static double round(double v) {
        return Math.round(v * 100d) / 100d;
    }
}
