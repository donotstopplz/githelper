package com.github.itisokey.githelper.window;

import cn.hutool.core.collection.CollectionUtil;
import com.github.lvlifeng.githelper.Bundle;
import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SingleSelectionModel;
import com.intellij.ui.awt.RelativePoint;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.branch.GitBranchUtil;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import com.github.itisokey.githelper.gitlab.helper.RepositoryHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lv LiFeng
 * @date 2022/1/2 15:56
 */
public class GitHelperWindow {

    private static final Logger LOG = Logger.getInstance(GitHelperWindow.class);
    private JTextField searchText;
    private JList repositoryList;
    private JList commonLocalBranchList;
    private JList commonRemoteBranchList;
    private JPanel gitHelperPanel;
    private JCheckBox allCheckBox;
    private JLabel localDefaultText;
    private JLabel remoteDefaultText;
    private JLabel repositoryDefaultText;
    private JLabel choosedSum;
    private JComboBox searchType;

    private List<GitRepository> gitRepositories;
    private List<GitLocalBranch> commonLocalBranches;
    private List<GitRemoteBranch> commonRemoteBranches;

    private Set<GitRepository> choosedRepositories = new HashSet<>();

    private GitBrancher gitBrancher;

    private List<GitRepository> filterRepositories = new ArrayList<>();

    public GitHelperWindow(Project project) {

        List<GitRepository> repositories = GitUtil.getRepositories(project).stream().collect(Collectors.toList());
        if (CollectionUtil.isEmpty(repositories)) {
            allCheckBox.setVisible(false);
            choosedSum.setVisible(false);
            return;
        }
        this.gitRepositories = repositories;
        this.gitBrancher = GitBrancher.getInstance(project);

        RepositoryHelper.sortRepositoriesByName(gitRepositories);
        initSearchText();
        initRepositoryList(null);
        initAllCheckBox();


    }

    private void initSearchText() {
        searchType.setModel(new DefaultComboBoxModel(
                Lists.newArrayList(
                        "Project Name",
                        "Local Branch Name",
                        "Remote Branch Name"
                ).toArray())
        );
        searchType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                initRepositoryList(searchText.getText());
                reInitLocalAndRemoteDataList();
            }
        });
        searchText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                String searchWord = ((JTextField) e.getSource()).getText();
                initRepositoryList(searchWord);
                reInitLocalAndRemoteDataList();
            }
        });

    }

    private void initAllCheckBox() {
        allCheckBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                if (StringUtils.equalsIgnoreCase(" ", Character.toString(e.getKeyChar()))) {
                    initAllCheckData((JCheckBox) e.getSource());
                }
            }
        });
        allCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() != 1) {
                    return;
                }
                initAllCheckData((JCheckBox) e.getSource());
            }
        });
    }

    private void initAllCheckData(JCheckBox checkBox) {
        if (checkBox.isSelected()) {
            choosedRepositories.addAll(filterRepositories);
            repositoryList.addSelectionInterval(0, filterRepositories.size());
        } else {
            choosedRepositories.clear();
            repositoryList.clearSelection();
        }
        setChoosedSum();
        assembleCommonLocalBranchDataList();
        assembleCommonRemoteBranchDataList();
    }


    private void initRepositoryList(String searchWord) {

        if (CollectionUtil.isEmpty(gitRepositories)) {
            hideRepositoryRelMenu();
        } else {
            showRepositoryRelMenu(searchWord);
            if (searchType.getSelectedItem().toString().contains("Project")) {
                filterRepositories = gitRepositories.stream()
                        .filter(o ->
                                (StringUtils.isNotEmpty(searchWord)
                                        && o.getRoot().getName().toLowerCase().contains(searchWord.toLowerCase()))
                                        || StringUtils.isEmpty(searchWord)
                        ).collect(Collectors.toList());
            }

            if (searchType.getSelectedItem().toString().contains("Local")) {
                filterRepositories = gitRepositories.stream()
                        .filter(o ->
                                (StringUtils.isNotEmpty(searchWord)
                                        && o.getBranches().getLocalBranches()
                                        .stream()
                                        .anyMatch(i -> i.getName().toLowerCase().contains(searchWord.toLowerCase())))
                                        || StringUtils.isEmpty(searchWord)
                        ).collect(Collectors.toList());
            }

            if (searchType.getSelectedItem().toString().contains("Remote")) {
                filterRepositories = gitRepositories.stream()
                        .filter(o ->
                                (StringUtils.isNotEmpty(searchWord)
                                        && o.getBranches().getRemoteBranches()
                                        .stream()
                                        .anyMatch(i -> i.getName().toLowerCase().contains(searchWord.toLowerCase())))
                                        || StringUtils.isEmpty(searchWord)
                        ).collect(Collectors.toList());
            }

            repositoryList.setListData(filterRepositories.stream()
                    .map(GitRepository::getRoot)
                    .map(VirtualFile::getName)
                    .collect(Collectors.toList())
                    .toArray());

            repositoryList.setCellRenderer(new LcheckBox());
            repositoryList.setEnabled(true);
            List<GitRepository> finalFilterRepositories = filterRepositories;
            repositoryList.setSelectionModel(new DefaultListSelectionModel() {
                @Override
                public void setSelectionInterval(int index0, int index1) {
                    if (super.isSelectedIndex(index0)) {
                        super.removeSelectionInterval(index0, index1);
                        choosedRepositories.remove(finalFilterRepositories.get(index0));
                    } else {
                        super.addSelectionInterval(index0, index1);
                        choosedRepositories.add(finalFilterRepositories.get(index0));
                        checkAll(finalFilterRepositories);
                    }
                    setChoosedSum();
                    assembleCommonLocalBranchDataList();
                    assembleCommonRemoteBranchDataList();
                }
            });
            if (CollectionUtil.isNotEmpty(choosedRepositories)) {
                repositoryList.setSelectedIndices(choosedRepositories.stream()
                        .map(o -> finalFilterRepositories.indexOf(o))
                        .mapToInt(Integer::valueOf)
                        .toArray());
                checkAll(filterRepositories);
            } else {
                repositoryList.clearSelection();
            }
        }
    }

    private void checkAll(List<GitRepository> filterRepositories) {
        if (choosedRepositories.size() == gitRepositories.size()
                && filterRepositories.size() == gitRepositories.size()) {
            allCheckBox.setSelected(true);
        } else {
            allCheckBox.setSelected(false);
        }
    }

    private void hideRepositoryRelMenu() {
        repositoryDefaultText.setVisible(true);
        repositoryList.setVisible(false);
        allCheckBox.setVisible(false);
        choosedSum.setVisible(false);
    }

    private void showRepositoryRelMenu(String searchWord) {
        repositoryDefaultText.setVisible(false);
        repositoryList.setVisible(true);
        allCheckBox.setVisible(true);
        choosedSum.setVisible(true);
    }

    private void setChoosedSum() {
        choosedSum.setText(String.format("(%s Selected)", choosedRepositories.size()));
    }

    private void assembleCommonRemoteBranchDataList() {
        commonRemoteBranches = GitBranchUtil.getCommonRemoteBranches(choosedRepositories);
        if (CollectionUtil.isEmpty(commonRemoteBranches)) {
            remoteDefaultText.setVisible(true);
            commonRemoteBranchList.setVisible(false);
            commonRemoteBranchList.setListData(new Object[]{});
        } else {
            remoteDefaultText.setVisible(false);
            commonRemoteBranchList.setVisible(true);
            List<String> filterBranches = commonRemoteBranches.stream()
                    .sorted(Comparator.comparing(GitRemoteBranch::getName))
                    .filter(o -> (searchType.getSelectedItem().toString().contains("Remote")
                            && StringUtils.isNotEmpty(searchText.getText()) && o.getName().contains(searchText.getText()))
                            || !searchType.getSelectedItem().toString().contains("Remote")
                            || (searchType.getSelectedItem().toString().contains("Remote")
                            && StringUtils.isEmpty(searchText.getText())))
                    .map(GitRemoteBranch::getName)
                    .collect(Collectors.toList());
            commonRemoteBranchList.setListData(filterBranches.toArray());
            commonRemoteBranchList.setCellRenderer(new LmenuItem());
            commonRemoteBranchList.setSelectionModel(new SingleSelectionModel() {

                @Override
                public void setSelectionInterval(int index0, int index1) {
                    super.setSelectionInterval(index0, index1);
                    JBPopupFactory.getInstance()
                            .createListPopup(new Lpopup(Lists.newArrayList(Bundle.message("checkout"),
                                    Bundle.message("newBranchFromSelected"),
                                    Bundle.message("delete")),
                                    gitBrancher,
                                    filterBranches.get(index0),
                                    choosedRepositories,
                                    true))
                            .show(new RelativePoint(commonRemoteBranchList,
                                    new Point(commonRemoteBranchList.getX() - 200,
                                            (int) commonRemoteBranchList.getMousePosition().getY())));
                }
            });
        }
    }

    private void assembleCommonLocalBranchDataList() {

        commonLocalBranches = GitBranchUtil.getCommonLocalBranches(choosedRepositories);
        if (CollectionUtil.isEmpty(commonLocalBranches)) {
            localDefaultText.setVisible(true);
            commonLocalBranchList.setVisible(false);
            commonLocalBranchList.setListData(new Object[]{});
        } else {
            localDefaultText.setVisible(false);
            commonLocalBranchList.setVisible(true);
            List<String> filterBranches = commonLocalBranches.stream()
                    .sorted(Comparator.comparing(GitLocalBranch::getName))
                    .filter(o -> (searchType.getSelectedItem().toString().contains("Local")
                            && StringUtils.isNotEmpty(searchText.getText()) && o.getName().contains(searchText.getText()))
                            || !searchType.getSelectedItem().toString().contains("Local")
                            || (searchType.getSelectedItem().toString().contains("Local")
                            && StringUtils.isEmpty(searchText.getText())))
                    .map(GitLocalBranch::getName)
                    .collect(Collectors.toList());
            commonLocalBranchList.setListData(filterBranches.toArray());
            commonLocalBranchList.setCellRenderer(new LmenuItem());
            commonLocalBranchList.setSelectionModel(new SingleSelectionModel() {
                @Override
                public void setSelectionInterval(int index0, int index1) {
                    super.setSelectionInterval(index0, index1);
                    JBPopupFactory.getInstance()
                            .createListPopup(new Lpopup(Lists.newArrayList(Bundle.message("checkout"),
                                    Bundle.message("newBranchFromSelected"),
                                    Bundle.message("delete")),
                                    gitBrancher,
                                    filterBranches.get(index0),
                                    choosedRepositories,
                                    false))
                            .show(new RelativePoint(commonLocalBranchList,
                                    new Point(commonLocalBranchList.getX() - 200,
                                            (int) commonLocalBranchList.getMousePosition().getY())));
                }
            });
        }

    }

    private void reInitLocalAndRemoteDataList() {
        assembleCommonLocalBranchDataList();
        assembleCommonRemoteBranchDataList();
    }


    public JPanel getGitHelperPanel() {
        return gitHelperPanel;
    }
}
