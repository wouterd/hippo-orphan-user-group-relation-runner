package com.hinttech.hippo.runner;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.onehippo.forge.jcrrunner.plugins.AbstractRunnerPlugin;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Wouter Danes
 */
public class OrphanUserGroupRelationDeleterRunner extends AbstractRunnerPlugin {

    private final static String QUERY_USER_EXISTS = "SELECT * FROM hipposys:user WHERE fn:name()='{}'";
    private static final String PROPERTY_HIPPOSYS_MEMBERS = "hipposys:members";
    private static final String NODE_TYPE_HIPPOSYS_GROUP = "hipposys:group";

    private static final List<String> USER_NAMES_TO_IGNORE = Arrays.asList("*");

    @Override
    public void visit(Node node) {
        try {
            Session session = node.getSession();
            processGroup(session, node);
        } catch (RepositoryException e) {
            getLogger().error("Something went wrong", e);
        }

        getLogger().info("========");
    }

    private void processGroup(Session session, Node node) throws RepositoryException {
        if (!node.isNodeType(NODE_TYPE_HIPPOSYS_GROUP)) {
            getLogger().error("Cannot process a node that is not a hipposys:group node");
            return;
        }
        getLogger().info("======== Processing group: {}", obtainGroupName(node));

        List<String> users = getAllUsersInGroup(node);

        if (users.isEmpty()) {
            getLogger().info("= No users in this group");
            return;
        }

        List<String> validatedUsers = obtainAllUsersThatExist(session, users);

        boolean usersChanged = validatedUsers.size() < users.size();

        if (!usersChanged) {
            getLogger().info("= All users in the group are valid");
            return;
        }

        boolean shouldCommit = Boolean.parseBoolean(getConfigValue("commit"));
        if (shouldCommit) {
            setMembersPropertyOnGroupNode(session, node, validatedUsers);
        } else {
            outputUserInformation(users, validatedUsers);
        }
    }

    private void outputUserInformation(List<String> users, List<String> validatedUsers) {
        getLogger().info("= Initial users in group: {}", users);
        getLogger().info("= Validated users in group: {}", validatedUsers);
    }

    private void setMembersPropertyOnGroupNode(Session session, Node node, List<String> validatedUsers) throws RepositoryException {
        getLogger().info("= Writing new users to group");
        node.setProperty(PROPERTY_HIPPOSYS_MEMBERS, validatedUsers.toArray(new String[validatedUsers.size()]));
        session.save();
    }

    private List<String> obtainAllUsersThatExist(Session session, List<String> users) throws RepositoryException {
        List<String> validatedUsers = new ArrayList<String>();
        for (String user : users) {
            if (shouldIgnoreUser(user) || userExists(session, user)) {
                validatedUsers.add(user);
            } else {
                getLogger().info("= Found user '{}' that doesn't exist anymore", user);
            }
        }
        return validatedUsers;
    }

    private List<String> getAllUsersInGroup(Node node) throws RepositoryException {
        if (!node.hasProperty(PROPERTY_HIPPOSYS_MEMBERS)) {
            return Collections.emptyList();
        }
        Value[] values = node.getProperty(PROPERTY_HIPPOSYS_MEMBERS).getValues();
        List<String> users = new ArrayList<String>(values.length);
        for (Value value : values) {
            String user = value.getString();
            users.add(user);
        }
        return users;
    }

    private String obtainGroupName(Node node) throws RepositoryException {
        String description = null;
        if (node.hasProperty("hipposys:description")) {
            description = node.getProperty("hipposys:description").getString();
        }
        String nodeName = node.getName();
        return StringUtils.isBlank(description) ? nodeName : description + "(" + nodeName + ")";
    }

    @SuppressWarnings("deprecation")
    boolean userExists(Session session, String username) throws RepositoryException {
        String encodedUsername = ISO9075.encode(username);
        String statement = QUERY_USER_EXISTS.replace("{}", encodedUsername);
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(statement, Query.SQL);
        return query.execute().getNodes().hasNext();
    }

    boolean shouldIgnoreUser(String username) {
        return USER_NAMES_TO_IGNORE.contains(username);
    }
}
