package ca.pfv.spmf.test;

import ca.pfv.spmf.algorithms.frequentpatterns.AlgoFCHM_Stream.AlgoFCHM_Stream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

public class MainTestFCHM_Stream {
    public static void main(String[] args) throws IOException {

        String input = fileToPath("dataset/Mushroom.txt");
        String output = ".//output.txt";
        int minutil = 4000;//最小效用阈值

        // 窗口中批次的数量
        int win_size = 4;

        // 一个批次中事务的数量
        int batch_size = 1000;
        AlgoFCHM_Stream algorithm = new AlgoFCHM_Stream(false, true);
        algorithm.runAlgorithm(
                input,
                minutil,
                win_size,
                batch_size,
                output);
        // Print statistics
        System.out.println("Minutil :" + minutil);
        System.out.println("Dataset :" + input);
        algorithm.printStats();
    }

    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = MainTestFCHM_Stream.class.getResource(filename);
        return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
    }
}

