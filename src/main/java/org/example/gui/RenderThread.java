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
    private long renderDelay = 800L;
    public RenderThread(Rendeble mainWindow){
        super();
        renderedWindow = mainWindow;
    }

    @Override
    public void run() {
        long millisPrev = System.currentTimeMillis() - renderDelay - renderDelay;
        while ((!Thread.currentThread().isInterrupted()) && threadLive && renderedWindow.isEnable()) {
            if (System.currentTimeMillis() - millisPrev > renderDelay) {
                millisPrev = System.currentTimeMillis();
                renderedWindow.renderData();
            }else{
                try {
                    Thread.sleep(renderDelay/ 2L);
                    //System.out.println("Sleep " + (Math.min((millisLimit / 3), 300L)) + " time limit is " + millisLimit);
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }
            }

        }
    }
}
