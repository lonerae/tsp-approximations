package gr.lonerae.utils;

public class CommonUtils {
    private CommonUtils(){}

    public static double round(double number) {
        return Math.round(number * 100.0) / 100.0;
    }
}
