package org.example.gui;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.time.LocalDateTime;

public class RenderThread implements Runnable{

    private LocalDateTime now = LocalDateTime.now();
    private final Rendeble renderedWindow;

    @Getter @Setter
    private boolean threadLive = true;

    @Getter
    private int renderDelay = 700;
    public RenderThread(Rendeble mainWindow){
        super();
        renderedWindow = mainWindow;
    }

    @Override
    public void run() {
        long millisLimit = renderDelay;
        long millisPrev = System.currentTimeMillis() - millisLimit - millisLimit;
        while ((!Thread.currentThread().isInterrupted()) && threadLive) {
            if (System.currentTimeMillis() - millisPrev > millisLimit) {
                millisPrev = System.currentTimeMillis();
                renderedWindow.renderData();
            }else{
                try {
                    Thread.sleep(Math.min((millisLimit / 3), 300L));
                    //System.out.println("Sleep " + (Math.min((millisLimit / 3), 300L)) + " time limit is " + millisLimit);
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }
            }

        }
    }
}
