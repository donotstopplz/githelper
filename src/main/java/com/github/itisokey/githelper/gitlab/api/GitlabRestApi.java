package com.github.itisokey.githelper.gitlab.api;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.AuthMethod;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.TokenType;
import org.gitlab.api.http.GitlabHTTPRequestor;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabNamespace;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabSession;
import org.gitlab.api.models.GitlabTag;
import org.gitlab.api.models.GitlabUser;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;


/**
 * @author Lv LiFeng
 * @date 2022/1/7 00:34
 */
public class GitlabRestApi {

    private String host;

    private GitlabAPI api;

    public GitlabRestApi() {
    }

    public GitlabRestApi(String host, String key) {
        this.host = host;
        reload(host, key);
    }

    public boolean reload(String host, String key) {
        if (host != null && key != null && !host.isEmpty() && !key.isEmpty()) {
            api = GitlabAPI.connect(host, key, TokenType.PRIVATE_TOKEN, AuthMethod.URL_PARAMETER);
            api.ignoreCertificateErrors(true);
            api.setConnectionTimeout(10 * 1000);
            api.setResponseReadTimeout(10 * 1000);
            return true;
        }
        return false;
    }

    public GitlabSession getSession() throws IOException {
        return api.getCurrentSession();
    }

    private void checkApi() throws IOException {
        if (api == null) {
            throw new IOException("please, configure plugin settings");
        }
    }

    public GitlabMergeRequest createMergeRequest(GitlabProject project, GitlabUser assignee, String from, String to, String title, String description, boolean removeSourceBranch) throws IOException {
        String tailUrl = "/projects/" + project.getId() + "/merge_requests";
        GitlabHTTPRequestor requestor = api.dispatch()
                .with("source_branch", from)
                .with("target_branch", to)
                .with("title", title)
                .with("description", description);
        if (removeSourceBranch) {
            requestor.with("remove_source_branch", true);
        }
        if (assignee != null) {
            requestor.with("assignee_id", assignee.getId());
        }

        return requestor.to(tailUrl, GitlabMergeRequest.class);
    }

    public GitlabProject getProject(Integer id) throws IOException {
        return api.getProject(id);
    }

    public List<GitlabBranch> loadProjectBranches(GitlabProject gitlabProject) throws IOException {
        return api.getBranches(gitlabProject);
    }

    public Collection<GitlabProject> getProjects() throws Throwable {
        checkApi();

        SortedSet<GitlabProject> result = new TreeSet<>(new Comparator<GitlabProject>() {
            @Override
            public int compare(GitlabProject o1, GitlabProject o2) {
                GitlabNamespace namespace1 = o1.getNamespace();
                String n1 = namespace1 != null ? namespace1.getName().toLowerCase() : "Default";
                GitlabNamespace namespace2 = o2.getNamespace();
                String n2 = namespace2 != null ? namespace2.getName().toLowerCase() : "Default";

                int compareNamespace = n1.compareTo(n2);
                return compareNamespace != 0 ? compareNamespace : o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        });

        List<GitlabProject> projects;
        try {
            projects = api.getMembershipProjects();
        } catch (Throwable e) {
            projects = Collections.emptyList();
        }
        projects = projects.stream().filter(project -> !Boolean.TRUE.equals(project.isArchived())).collect(Collectors.toList());
        result.addAll(projects);

        return result;
    }

    public Collection<GitlabUser> searchUsers(GitlabProject project, String text) throws IOException {
        checkApi();
        List<GitlabUser> users = new ArrayList<>();
        if (text != null) {
            String tailUrl = GitlabProject.URL + "/" + project.getId() + "/users" + "?search=" + URLEncoder.encode(text, "UTF-8");
            GitlabUser[] response = api.retrieve().to(tailUrl, GitlabUser[].class);
            users = Arrays.asList(response);
        }
        return users;
    }

    public GitlabUser getCurrentUser() {
        try {
            checkApi();
            return api.getUser();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<GitlabBranch> getBranchesByProject(GitlabProject project) {
        return api.getBranches(project);
    }

    public List<GitlabTag> getTagsByProject(GitlabProject project) {
        return api.getTags(project);
    }

    public List<GitlabUser> getActiveUsers() {
        try {
            return api.getUsers()
                    .stream()
                    .filter(o -> StringUtils.equalsIgnoreCase(o.getState(), "active"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Lists.newArrayList();
        }
    }

    public List<GitlabMergeRequest> getOpenMergeRequest(Serializable projectId) throws IOException {
        return api.getOpenMergeRequests(projectId);
    }

    public GitlabMergeRequest updateMergeRequest(Serializable projectId, Integer mergeRequestIid, String targetBranch,
                                                 Integer assigneeId, String title, String description, String stateEvent,
                                                 String labels) throws IOException {
        return api.updateMergeRequest(projectId, mergeRequestIid, targetBranch, assigneeId, title, description, stateEvent, labels);
    }

    public GitlabMergeRequest acceptMergeRequest(Serializable projectId, Integer mergeRequestIid, String mergeCommitMessage) throws IOException {
        return api.acceptMergeRequest(projectId, mergeRequestIid, mergeCommitMessage);
    }

    public GitlabTag addTag(Serializable projectId, String tagName, String ref, String message, String releaseDescription) throws IOException {
        return api.addTag(projectId, tagName, ref, message, releaseDescription);
    }

    public GitlabProject getProjectByNamespaceAndName(String namespace, String projectName) throws IOException {
        return api.getProject(namespace, projectName);
    }
}
