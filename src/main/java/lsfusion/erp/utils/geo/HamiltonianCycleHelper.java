package lsfusion.erp.utils.geo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HamiltonianCycleHelper {
    private int[][] adjMatrix;
    private int vertexCnt;
    final static private int exactAlgoVertexLimit = 17;         
    
    public HamiltonianCycleHelper(int[][] adjMatrix) {
        this.adjMatrix = adjMatrix;
        vertexCnt = adjMatrix.length;
    }
    
    public int[] execute() {
        if (vertexCnt <= exactAlgoVertexLimit) {
            return dynamicProgrammingSolution();
        } else {
            return nearestPointApproximation();
        }
    }
    
    private int[] dynamicProgrammingSolution() {
        int memo[][] = new int[vertexCnt][1 << vertexCnt];
        for (int[] row : memo) {
            Arrays.fill(row, -1);
        }
        
        memo[0][1] = 0;
        
        for (int mask = 1; mask < (1 << vertexCnt); ++mask) {
            for (int cur = 1; cur < vertexCnt; ++cur) {
                if ((mask & (1 << cur)) != 0) {
                    for (int prev = 0; prev < vertexCnt; ++prev) {
                        if ((mask & (1 << prev)) != 0) {
                            if (memo[prev][mask ^ (1 << cur)] >= 0) {
                                memo[cur][mask] = Math.min(memo[cur][mask], memo[prev][mask ^ (1 << cur)] + adjMatrix[prev][cur]);
                            }
                        }
                    }
                }
            }
        }

        int res = Integer.MAX_VALUE;
        for (int last = 1; last < vertexCnt; ++last) {
            res = Math.min(res, memo[last][(1 << vertexCnt) - 1] + adjMatrix[last][0]);
        }

        // reconstruct path
        int curMask = (1 << vertexCnt) - 1;
        int[] tour = new int[vertexCnt];
        int last = 0;
        for (int i = vertexCnt - 1; i >= 1; --i) {
            int best = -1;
            for (int j = 1; j < vertexCnt; ++j) {
                if ((curMask & (1 << j)) != 0 && (best == -1 || memo[best][curMask] + adjMatrix[best][last] > memo[j][curMask] + adjMatrix[j][last])) {
                    best = j;
                }
            }
            tour[i] = best;
            curMask ^= (1 << best);
            last = best;
        }
        return tour;
    }  
    
    private int[] nearestPointApproximation() {
        List<Integer> tour = new ArrayList<>();
        boolean inTour[] = new boolean[vertexCnt];
        tour.add(0);
        inTour[0] = true;
        
        for (int iter = 1; iter < vertexCnt; ++iter) {
            int best = 0;
            boolean first = true;
            int minVertexFound = 0;
            int vertexConnectTo = 0;
            
            for (int i = 0; i < tour.size(); ++i) {
                for (int next = 0; next < vertexCnt; ++next) {
                    if (!inTour[next]) {
                        int increase; 
                        if (i + 1 == tour.size()) {
                            increase = adjMatrix[tour.get(i)][next];    
                        } else {
                            increase = adjMatrix[tour.get(i)][next] + adjMatrix[next][tour.get(i+1)] - adjMatrix[tour.get(i)][tour.get(i+1)];
                        }
                        if (first || best > increase) {
                            best = increase;
                            first = false;
                            minVertexFound = next;
                            vertexConnectTo = i;
                        }
                    }
                }
            }
            
            tour.add(vertexConnectTo + 1, minVertexFound);
            inTour[minVertexFound] = true;
        }
        
        int[] tourArray = new int[vertexCnt];
        for (int i = 0; i < vertexCnt; ++i) {
            tourArray[i] = tour.get(i);
        }
        return tourArray;
    }
            
}