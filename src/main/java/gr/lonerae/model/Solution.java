package gr.lonerae.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;

@Data
@AllArgsConstructor
public class Solution {
    private double length;
    private ArrayList<Point> sequence;
    private int portalsPerSide;
    private double e;
    private boolean best;
    private double[] dimensions;
}