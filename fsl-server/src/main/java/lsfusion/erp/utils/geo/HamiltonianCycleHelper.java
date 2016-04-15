package lsfusion.erp.utils.geo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HamiltonianCycleHelper {
    private int[][] adjMatrix;
    private int vertexCnt;
    final static private int exactAlgoVertexLimit = 18;         
    
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
        short prevVertex[][] = new short[vertexCnt][1 << vertexCnt]; 
        for (int[] row : memo) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }
        
        memo[0][1] = 0;
        
        for (int mask = 1; mask < (1 << vertexCnt); ++mask) {
            for (int cur = 1; cur < vertexCnt; ++cur) {
                if ((mask & (1 << cur)) != 0) {
                    for (int prev = 0; prev < vertexCnt; ++prev) {
                        if (prev != cur && (mask & (1 << prev)) != 0) {
                            int prevLen = memo[prev][mask ^ (1 << cur)];
                            if (prevLen < Integer.MAX_VALUE) {
                                if (prevLen + adjMatrix[prev][cur] < memo[cur][mask]) {
                                    memo[cur][mask] = prevLen + adjMatrix[prev][cur];
                                    prevVertex[cur][mask] = (short) prev;
                                }
                            }
                        }
                    }
                }
            }
        }

        int res = Integer.MAX_VALUE;
        int curMask = (1 << vertexCnt) - 1;
        int bestLast = 0;
        
        for (int last = 1; last < vertexCnt; ++last) {
            if (memo[last][curMask] >= 0) {
                int length = memo[last][curMask] + adjMatrix[last][0];
                if (res > length) {
                    res = length;
                    bestLast = last; 
                }
            }
        }
        
        // reconstruct path
        int[] tour = new int[vertexCnt];
        int last = bestLast;
        
        for (int i = vertexCnt - 1; i > 0; --i) {
            tour[i] = last;
            int newMask = curMask ^ (1 << last);             
            last = prevVertex[last][curMask];
            curMask = newMask; 
        }
        return tour;
    }  
    
    private int[] nearestPointApproximation() {
        List<Integer> tour = new ArrayList<>();
        boolean inTour[] = new boolean[vertexCnt];
        tour.add(0);
        inTour[0] = true;
        
        for (int iter = 1; iter < vertexCnt; ++iter) {
            int best = -1;
            boolean first = true;
            int minVertexFound = 0;
            int vertexConnectTo = 0;
            
            for (int i = 0; i < tour.size(); ++i) {
                for (int next = 0; next < vertexCnt; ++next) {
                    if (!inTour[next]) {
                        int increase; 
                        if (i + 1 == tour.size()) {
                            increase = adjMatrix[tour.get(i)][next] + adjMatrix[next][0] - adjMatrix[tour.get(i)][0];    
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