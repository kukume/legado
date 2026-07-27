package me.kuku.legado.gui.ui;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColorPicker;
import com.intellij.ui.JBColor;
import me.kuku.legado.state.SettingsService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SettingUI {
    private JPanel rootPanel;

    private JLabel textBodyFontColorLabel;

    private JSpinner textBodyFontSizeSpinner;
    private JCheckBox enableErrorLogCheckBox;
    private JTextField cookieField;
    private JTextField addressTextField;

    public SettingUI() {
        // 正文大小输入范围
        textBodyFontSizeSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));

        // 正文字体颜色选择的点击事件
        textBodyFontColorLabel.addMouseListener(chooseColorMouseListener());
    }

    @NotNull
    private MouseAdapter chooseColorMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Color newColor = ColorPicker.showDialog(rootPanel, textBodyFontColorLabel.getText() + " Color", textBodyFontColorLabel.getForeground(), true, null, true);
                if (newColor != null) {
                    textBodyFontColorLabel.setForeground(newColor);
                }
            }
        };
    }

    public JComponent getComponent() {
        return rootPanel;
    }

    public void readSettings() {
        SettingsService settingsService = SettingsService.getInstance();
        String textBodyFontColor = settingsService.getState().getTextBodyFontColor();
        int textBodyFontSize = settingsService.getState().getTextBodyFontSize();
        String cookie = settingsService.getState().getCookie();
        boolean enableErrorLog = settingsService.getState().getEnableErrorLog();
        String address = settingsService.getState().getAddress();

        if (StringUtil.isNotEmpty(textBodyFontColor)) {
            int rgb = Integer.parseInt(textBodyFontColor);
            textBodyFontColorLabel.setForeground(new JBColor(new Color(rgb), new Color(rgb)));
        }

        if (textBodyFontSize > 0) {
            textBodyFontSizeSpinner.setValue(textBodyFontSize);
        }

        if (StringUtil.isNotEmpty(cookie)) {
            cookieField.setText(cookie);
        } else {
            cookieField.setText("");
        }

        if (StringUtil.isNotEmpty(address)) {
            addressTextField.setText(address);
        } else {
            addressTextField.setText("https://api.langge.cf");
        }

        enableErrorLogCheckBox.setSelected(enableErrorLog);
    }


    public void saveSettings() {
        SettingsService settingsService = SettingsService.getInstance();
        settingsService.getState().setTextBodyFontColor(String.valueOf(textBodyFontColorLabel.getForeground().getRGB()));
        settingsService.getState().setTextBodyFontSize(Integer.parseInt(String.valueOf(textBodyFontSizeSpinner.getValue())));
        settingsService.getState().setTextBodyFontName(textBodyFontSizeSpinner.getFont().getName());
        settingsService.getState().setEnableErrorLog(enableErrorLogCheckBox.isSelected());
        settingsService.getState().setCookie(String.valueOf(cookieField.getText()).trim());
        String address = addressTextField.getText();
        if (StringUtil.isEmpty(address)) {
            address = "https://api.langge.cf";
        }
        settingsService.getState().setAddress(address.trim());
    }
}
