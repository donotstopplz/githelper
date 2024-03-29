package com.github.itisokey.githelper.gitlab.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.github.itisokey.githelper.gitlab.bean.Result;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author Lv LiFeng
 * @date 2022/1/11 23:24
 */
public class ResultDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextArea result;
    private List<Result> results;

    public ResultDialog(List<Result> results, String title) {
        super(true);
        setTitle(title);
        this.results = results;
        init();
        result.setBorder(null);
        result.setSelectedTextColor(Color.GREEN);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setText(results.stream()
                .map(Result::toString)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("None")
        );
        result.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                selectAll();
            }
        });
    }

    private void selectAll() {
        if (StringUtils.isNotBlank(result.getText())) {
            result.setSelectionStart(0);
            result.setSelectionEnd(result.getText().length());
            result.copy();
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
