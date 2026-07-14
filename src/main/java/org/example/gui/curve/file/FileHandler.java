package org.example.gui.curve.file;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

@Slf4j
public class FileHandler {
    @Getter
    private String filePath;
    @Getter
    File selectedFileToOpen = null;


    public File selectFileToOpen() throws CurveFileAccessException {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Poly files (*.curve, *.340)", "curve", "340");

        fileChooser.setFileFilter(filter);
        if (filePath != null) {
            fileChooser.setCurrentDirectory(new File(filePath));
        }
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if(selectedFile == null){
                log.error("Файл не выбран");
                throw new CurveFileAccessException("Файл не выбран");
            }
            if(selectedFile.exists() != true){
                log.error("Выбранный файл не существует");
                throw new CurveFileAccessException("Выбранный файл не существует");
            }
            if(selectedFile.canRead() != true){
                log.error("Выбранный файл недоступен для чтения");
                throw new CurveFileAccessException("Выбранный файл недоступен для чтения");
            }
            selectedFileToOpen = selectedFile;
            filePath = selectedFileToOpen.getAbsolutePath();
            return selectedFileToOpen;
        }
        return null;
    }
}
