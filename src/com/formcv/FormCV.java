package com.formcv;

import org.opencv.core.Mat;

public class FormCV {

    public static void main(String[] args) {
        Forms a = new Forms();
        Mat c = a.imgNormalizer(a.imgUndistort("img.jpg"));
        a.displayImage(a.getImgResize());
        a.displayImage(a.getImgContour());
        a.displayImage(a.getImgNormal());


    }
}
