package com.github.itisokey.githelper.gitlab.settings;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.EnumComboBoxModel;
import com.github.lvlifeng.githelper.bean.GitlabServer;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * @author Lv LiFeng
 * @date 2022/1/23 10:16
 */
public class ServerConfiguration extends DialogWrapper {

    private GitlabServer gitlabServer;
    private GitLabSettingsState settingsState = GitLabSettingsState.getInstance();
    private ExecutorService executor = Executors.newSingleThreadExecutor();


    private JPanel panel;
    private JTextField apiURl;
    private JTextField token;
    private JButton tokenPage;
    private JComboBox checkoutMethod;

    protected ServerConfiguration( GitlabServer gitlabServer) {
        super(false);
        if (gitlabServer == null) {
            this.gitlabServer = new GitlabServer();
        } else {
            this.gitlabServer = gitlabServer;
        }
        init();
        setTitle("GitLab Server Details");
    }

    private static boolean isValidUrl(String s) {
        try {
            URI uri = new URI(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void init() {
        super.init();

        setupModel();
        fillFormFromDto();
        setupListeners();

    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        final String apiUrl = apiURl.getText();
        final String tokenString = token.getText();
        if (StringUtils.isBlank(apiUrl)) {
            return new ValidationInfo(SettingError.URL_NOT_NULL.message(), apiURl);
        }
        if (StringUtils.isBlank(tokenString)) {
            return new ValidationInfo(SettingError.TOKEN_NOT_NULL.message(), token);
        }
        try {
            if (isNotBlank(apiUrl) && isNotBlank(tokenString)) {
                if (!isValidUrl(apiUrl)) {
                    return new ValidationInfo(SettingError.NOT_A_URL.message(), apiURl);
                } else {

                    Future<ValidationInfo> infoFuture = executor.submit(() -> {
                        try {
                            settingsState.isApiValid(apiUrl, tokenString);
                            return null;
                        } catch (UnknownHostException e) {
                            return new ValidationInfo(SettingError.SERVER_CANNOT_BE_REACHED.message(), apiURl);
                        } catch (IOException e) {
                            return new ValidationInfo(SettingError.INVALID_API_TOKEN.message(), token);
                        }
                    });
                    try {
                        ValidationInfo info = infoFuture.get(5000, TimeUnit.MILLISECONDS);
                        return info;
                    } catch (Exception e) {
                        return new ValidationInfo(SettingError.GENERAL_ERROR.message());
                    }
                }
            }
        } catch (Exception e) {
            return new ValidationInfo(SettingError.GENERAL_ERROR.message());
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        gitlabServer.setApiUrl(apiURl.getText());
        gitlabServer.setApiToken(token.getText());
        gitlabServer.setRepositoryUrl(ApiToRepoUrlConverter.convertApiUrlToRepoUrl(apiURl.getText()));
        gitlabServer.setPreferredConnection(GitlabServer.CloneType.values()[checkoutMethod.getSelectedIndex()]);
        gitlabServer.setValidFlag(true);
        settingsState.addServer(gitlabServer);
    }

    private void setupListeners() {
        tokenPage.addActionListener(e -> openWebPage(generateHelpUrl()));
        onServerChange();
        apiURl.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onServerChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onServerChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onServerChange();
            }
        });
    }

    private void setupModel() {
        checkoutMethod.setModel(new EnumComboBoxModel(GitlabServer.CloneType.class));
    }

    private void fillFormFromDto() {
        checkoutMethod.setSelectedIndex(gitlabServer.getPreferredConnection().ordinal());
        apiURl.setText(gitlabServer.getApiUrl());
        token.setText(gitlabServer.getApiToken());
    }

    private void openWebPage(String uri) {
        if (StringUtils.isBlank(uri)) {
            return;
        }
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(uri));
            } catch (Exception ignored) {
            }
        }
    }

    private String generateHelpUrl() {
        final String hostText = apiURl.getText();
        StringBuilder helpUrl = new StringBuilder();
        helpUrl.append(hostText);
        if (!hostText.endsWith("/")) {
            helpUrl.append("/");
        }
        helpUrl.append("profile/personal_access_tokens");
        return helpUrl.toString();
    }

    private void onServerChange() {
        ValidationInfo validationInfo = doValidate();
        if (validationInfo == null || (!validationInfo.message.equals(SettingError.NOT_A_URL.message))) {
            tokenPage.setEnabled(true);
            tokenPage.setToolTipText("API Key can be find in your profile setting inside GitLab Server: \n" + generateHelpUrl());
        } else {
            tokenPage.setEnabled(false);
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }
}
