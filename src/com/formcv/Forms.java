package com.formcv;

import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Forms {

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private static int maxheight = 1024;
    private Mat imgresize = new Mat();
    private Mat imgcontour = new Mat();
    private Mat imgnormal = new Mat();
    private Boolean hasMaxContour = false;
    private Boolean undistortSuccess = false;
    private Boolean transformSuccess = false;
    private int w = 37 * 16;
    private int h = 47 * 21;
    private int f = 8;

    public Forms() {
    }

    public Mat imgUndistort(String file) {
        Mat img = imgRead(file);
        Mat imgGray = new Mat();
        Mat imgResize = new Mat();
        Mat imgBlur = new Mat();
        Mat imgThresh = new Mat();
        Mat imgInvThresh = new Mat();
        Mat imgUndistort = new Mat();

        Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_BGR2GRAY);
        Size size = imgGray.size();
        double width = size.width;
        double height = size.height;
        double maxwidth = this.maxheight/(width/height);
        Imgproc.resize(imgGray, imgResize, new Size(this.maxheight, maxwidth), 0, 0, Imgproc.INTER_AREA);
        this.imgresize = imgResize;
        Imgproc.GaussianBlur(imgResize, imgBlur, new Size(9,9), 0);
        Imgproc.adaptiveThreshold(imgBlur,
                imgThresh,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,3,2);
        Imgproc.adaptiveThreshold(imgBlur,
                imgInvThresh,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,3,2);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(imgInvThresh,
                contours,
                new Mat(),
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                new Point(3,2));
        Iterator<MatOfPoint> iterator = contours.iterator();
        double maxArea = 0;

        MatOfPoint2f maxContour = new MatOfPoint2f();
        while (iterator.hasNext()){
            MatOfPoint contour = iterator.next();
            double area = Imgproc.contourArea(contour);
            if(area > maxArea) {
                maxArea = area;
                maxContour = new MatOfPoint2f();
                contour.convertTo(maxContour, CvType.CV_32F);
                this.hasMaxContour = true;
            }
        }

        if(this.hasMaxContour) {
            double epsilon = 0.1 * Imgproc.arcLength(maxContour, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(maxContour, approx, epsilon, true);
            Moments moment = Imgproc.moments(approx);
            int x = (int) (moment.get_m10() / moment.get_m00());
            int y = (int) (moment.get_m01() / moment.get_m00());

            //SORT POINTS RELATIVE TO CENTER OF MASS
            Point[] sortedPoints = new Point[4];

            double[] data;
            int count = 0;
            for(int i=0; i<approx.rows(); i++){
                data = approx.get(i, 0);
                double datax = data[0];
                double datay = data[1];
                if(datax < x && datay < y){
                    sortedPoints[0]=new Point(datax,datay);
                    count++;
                }else if(datax > x && datay < y){
                    sortedPoints[1]=new Point(datax,datay);
                    count++;
                }else if (datax < x && datay > y){
                    sortedPoints[2]=new Point(datax,datay);
                    count++;
                }else if (datax > x && datay > y){
                    sortedPoints[3]=new Point(datax,datay);
                    count++;
                }

            }

            MatOfPoint2f src = new MatOfPoint2f(
                    sortedPoints[0],
                    sortedPoints[1],
                    sortedPoints[2],
                    sortedPoints[3]);

            MatOfPoint2f dst = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(this.w,0),
                    new Point(0,this.h),
                    new Point(this.w,this.h)
            );

            List<MatOfPoint> approxContourList = new ArrayList<>();
            MatOfPoint approxContourInt = new MatOfPoint();
            approx.convertTo(approxContourInt, CvType.CV_32S);
            approxContourList.add(approxContourInt);
            Imgproc.cvtColor(imgResize, this.imgcontour, Imgproc.COLOR_GRAY2BGR);
            Imgproc.drawContours(this.imgcontour, approxContourList, 0, new Scalar(0, 255, 0), 4);
            Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);
            Imgproc.warpPerspective(this.imgresize, imgUndistort, warpMat, new Size(this.w, this.h));
            this.undistortSuccess = true;
            return imgUndistort;
        }
        else {
            return new Mat(1,1,CvType.CV_8UC1);
        }
    }

    public Mat imgNormalizer(Mat img) {
        Mat output = new Mat();
        if(undistortSuccess) {
            if(img.channels() == 3) {
                Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
            }
            Imgproc.adaptiveThreshold(img, img, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 17, 25);
            Imgproc.GaussianBlur(img, img, new Size(13, 13), 0);
            Imgproc.threshold(img, img, 180, 255, Imgproc.THRESH_BINARY);
            this.imgnormal = img;
            Imgproc.resize(img, img, new Size(37, 95*this.f), 0,0, Imgproc.INTER_AREA);
            Imgproc.threshold(img, img, 180, 255, Imgproc.THRESH_BINARY);

            Rect cutArea = new Rect(new Point(1, this.f), new Point(img.width()-1, img.height()-this.f));
            img = new Mat(img, cutArea);
            output = img;
            this.transformSuccess = true;
        }
        return output;
    }

    public static void displayImage(Image img2) {
        ImageIcon icon=new ImageIcon(img2);
        JFrame frame=new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(img2.getWidth(null)+50, img2.getHeight(null)+50);
        JLabel lbl=new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void displayImage(Mat image) {
        Mat img1 = image;
        if(img1.channels() == 1) {
            Imgproc.cvtColor(img1, img1, Imgproc.COLOR_GRAY2BGR);
        }
        Image img2 = matToImage(img1);
        ImageIcon icon=new ImageIcon(img2);
        JFrame frame=new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(img2.getWidth(null)+50, img2.getHeight(null)+50);
        JLabel lbl=new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private static Mat imgRead(String filepath) {
        BufferedImage img = null;
        Mat output = new Mat();
        try {
            img = ImageIO.read(new File(filepath));
            img = rotateClockwise90(img);
            output = imageToMat(img);
        } catch (
                IOException e) {
        }
        return output;
    }

    private static BufferedImage rotateClockwise90(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage dest = new BufferedImage(height, width, src.getType());

        Graphics2D graphics2D = dest.createGraphics();
        graphics2D.translate((height - width) / 2, (height - width) / 2);
        graphics2D.rotate(Math.PI / 2, height / 2, width / 2);
        graphics2D.drawRenderedImage(src, null);

        return dest;
    }

    public static Mat imageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    public static BufferedImage matToImage(Mat imgContainer) {
        MatOfByte byteMatData = new MatOfByte();
        //image formatting
        Imgcodecs.imencode(".jpg", imgContainer,byteMatData);
        // Convert to array
        byte[] byteArray = byteMatData.toArray();
        BufferedImage img = null;
        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            //load image
            img = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return img;
    }

    public Mat getImgResize() {
        return this.imgresize;
    }

    public Mat getImgContour() {
        return this.imgcontour;
    }

    public Mat getImgNormal() {
        return this.imgnormal;
    }
}