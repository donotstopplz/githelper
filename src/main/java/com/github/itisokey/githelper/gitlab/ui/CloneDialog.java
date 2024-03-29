package com.github.itisokey.githelper.gitlab.ui;

import com.github.itisokey.githelper.gitlab.bean.ProjectDto;
import com.github.itisokey.githelper.gitlab.common.GitCheckoutProvider;
import com.github.lvlifeng.githelper.Bundle;
import com.github.lvlifeng.githelper.icons.Icons;
import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import git4idea.commands.Git;
import com.github.itisokey.githelper.gitlab.common.Notifier;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @author Lv LiFeng
 * @date 2022/1/8 10:59
 */
public class CloneDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(CloneDialog.class);
    private JPanel contentPane;
    private JPanel clonePane;
    private JTextField directory;
    private JLabel directoryButton;
    private final Set<ProjectDto> selectedProjectList;
    private CheckoutProvider.Listener checkoutListener;
    private final Project project;
    private VirtualFile destinationParent;

    protected CloneDialog(Project project, Set<ProjectDto> selectedProjectList) {
        super(true);
        init();
        setTitle(Bundle.message("cloneDialogTitle"));
        this.selectedProjectList = selectedProjectList;
        this.project = project;
        initDefaultDirectory();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        ValidationInfo destinationValidation = CloneDvcsValidationUtils.createDestination(directory.getText());
        if (destinationValidation != null) {
            return destinationValidation;
        }
        LocalFileSystem lfs = LocalFileSystem.getInstance();
        File file = new File(directory.getText());
        destinationParent = lfs.findFileByIoFile(file);
        if (destinationParent == null) {
            destinationParent = lfs.refreshAndFindFileByIoFile(file);
        }
        if (destinationParent == null) {
            return new ValidationInfo("Clone Failed. Destination doesn't exist", directory);
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        dispose();
        VirtualFile finalDestinationParent = destinationParent;
        checkoutListener = ProjectLevelVcsManager.getInstance(project).getCompositeCheckoutListener();
        StringBuilder info = new StringBuilder();
        selectedProjectList.forEach(s -> {
            GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, finalDestinationParent,
                    s.getSshUrl(), s.getName(), directory.getText());
            info.append(s.getName()).append(" ").append(s.getSshUrl()).append(" clone successfully!").append("\n");
        });
        Notifier.notify(project, info, null, null);
    }

    private void initDefaultDirectory() {
        directoryButton.setIcon(Icons.DirectoryDir);
        directoryButton.setBorder(null);
        directory.setText(project.getBasePath());
        directoryButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                File file = new File(project.getBasePath());
                LocalFileSystem lfs = LocalFileSystem.getInstance();
                VirtualFile preSelect = lfs.findFileByIoFile(file);
                FileChooser.chooseFiles(new FileChooserDescriptor(false,
                                true,
                                false,
                                false,
                                false,
                                false),
                        project,
                        preSelect,
                        new Consumer<List<VirtualFile>>() {
                            @Override
                            public void consume(List<VirtualFile> virtualFiles) {
                                directory.setText(virtualFiles.get(0).getPath());
                            }
                        });
            }
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }
}
