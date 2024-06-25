package com.example.citizenweatherforecast;

import org.tensorflow.lite.Interpreter;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class NNHandler {
    private Interpreter tflite;
    public NNHandler(Context context) {
        try {
            tflite = new Interpreter(loadModelFile(context, "model.tflite"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private float[][][] prepareData(List<List<Float>> input, float[] means, float[] stds){
        float[][][] arrayInput = new float[1][input.size()][input.get(0).size()];
        for (int i = 0; i < input.size(); i++) {
            for (int j = 0; j < input.get(i).size(); j++) {
                arrayInput[0][i][j] = input.get(i).get(j);
            }
        }

        for (int j = 0; j < arrayInput[0].length; j++) {
            for (int k = 0; k < arrayInput[0][j].length; k++) {
                switch (k) {
                    case 0:
                        arrayInput[0][j][k] = (arrayInput[0][j][k] - means[0]) / stds[0];
                        break;
                    case 1:
                        arrayInput[0][j][k] = (arrayInput[0][j][k] - means[1]) / stds[1];
                        break;
                    case 2:
                        arrayInput[0][j][k] = (arrayInput[0][j][k] - means[2]) / stds[2];
                        break;

                }
            }
        }

        return arrayInput;
    }

    public float[] postProcessData(float[] data, float[] means, float[] stds){
        data[0] = data[0] * stds[0] + means[0];
        data[1] = data[1] * stds[1] + means[1];
        data[2] = data[2] * stds[2] + means[2];
        data[3] = data[3] * stds[3] + means[3];

        return data;
    }

    private static float calculateMean(float[] array) {
        float sum = 0.0f;
        for (float num : array) {
            sum += num;
        }
        return sum / array.length;
    }

    private static float calculateStandardDeviation(float[] array, float mean) {
        float sumOfSquares = 0.0f;
        for (float num : array) {
            sumOfSquares += (num - mean) * (num - mean);
        }
        return (float) Math.sqrt(sumOfSquares / (array.length - 1));
    }

    public float[] runInferenceSingleOutput(List<List<Float>> input) {
        float[][][] initArrayInput = new float[1][input.size()][input.get(0).size()];
        for (int i = 0; i < input.size(); i++) {
            for (int j = 0; j < input.get(i).size(); j++) {
                initArrayInput[0][i][j] = input.get(i).get(j);
            }
        }

                                            //Temperatur, Humidity, pressure, rain
        float[] means = new float[4];
        float[] stds = new float[4];

        float[] temps = new float[input.size()];
        float[] hums = new float[input.size()];
        float[] press = new float[input.size()];
        float[] rain = new float[input.size()];

        for (int i = 0; i < input.size(); i++) {
            temps[i] = input.get(i).get(0);
            hums[i] = input.get(i).get(1);
            press[i] = input.get(i).get(2);
            rain[i] = input.get(i).get(3);
        }

        means[0] = calculateMean(temps);
        means[1] = calculateMean(hums);
        means[2] = calculateMean(press);

        stds[0] = calculateStandardDeviation(temps, means[0]);
        stds[1] = calculateStandardDeviation(hums, means[1]);
        stds[2] = calculateStandardDeviation(press, means[2]);

        float[][][] arrayInput = prepareData(input, means, stds);

        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(6 * 8 *Float.BYTES);
        inputBuffer.order(ByteOrder.nativeOrder());

        for (float[] row : arrayInput[0]) {
            for (float value : row) {
                inputBuffer.putFloat(value);
            }
        }

        float[][] output = new float[1][4];
        tflite.run(arrayInput, output);
        float[] processedOutput = postProcessData(output[0], means, stds);
        return processedOutput;
    }

    public List<List<Float>> runInferenceMultipleOutput(List<List<Float>> input) {
        List<List<Float>> output = new ArrayList<>();
        tflite.run(input, output);
        return output;
    }
}
