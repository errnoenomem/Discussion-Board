package cse310source;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class CSE310Server {

    private static BufferedWriter writer;
    private static BufferedReader fromClient;
    private static Scanner scanner;
    private static Scanner scanner2; //possibly uneeded
    private static String user;
    private static String clientInput;
    private static String temp;
    private static String group;
    private static boolean freeBool;
    private static boolean freeBool2;
    private static final int serverPort = 47374;
    private static BufferedWriter toClient;
    private static int clientNum;
    private static int num;
    private static int num2;
    private static int num3;
    private static int num4;
    private static int counter;
    private static ArrayList<Integer> numList;
    private static ArrayList<String> postList;
    private static ArrayList<String> combinedPostList;
    private static String subscribedPath;
    private static String groupsPath = "test/groups/allgroups.txt";
    private static String tempPath;
    private static String SAGPath;
    private static String dirPath;
    private static String unreadPath;
    private static String readPath;
    private static String groupPostsPath;

    public static void main(String[] args) throws IOException { //set up serverport and listener
        ServerSocket serverSocket = null;
        try {//attempts to listen to port #
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Server listening on port " + serverPort);
        } catch (IOException e) {
            System.err.println("Could not listen on port " + serverPort);
            System.exit(1);
        }
        Socket clientSocket = null;
        try {//accept clients connecting with port #
            clientSocket = serverSocket.accept();
            System.out.println("Client accepted");
            fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            toClient = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException e) {
            System.err.println("Client failed to accept");
            System.exit(1);
        }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        while (true) { //reactions to clientInput
            while ((clientInput = fromClient.readLine()) != null) {
                if (clientInput.startsWith("login")) {
                    if (!authenticateUser()) {
                        writeToClient("Invalid login, please try again");
                    }
                } else if (clientInput.equals("help")) {
                    help();
                } else if (clientInput.equals("protocol")) {
                    writeToClient(getProtocol(SAGPath));
                } else if (clientInput.equals("protocol2")) {
                    writeToClient(getProtocol(subscribedPath));
                } else if (clientInput.startsWith("ag")) {
                    freeBool2 = false;
                    num4 = 1;
                    dirPath = SAGPath;
                    if (clientInput.length() > 3) {
                        num = Integer.parseInt(clientInput.substring(3));
                        allGroups(true, num, dirPath);
                        counter = num;
                    } else if (clientInput.length() == 2) {
                        counter = 200;
                        num = 21;
                        allGroups(false, 0, dirPath);
                    }
                } else if (clientInput.startsWith("s ")) {
                    loadNumList();
                    subscribe(numList);
                    sortSubscribed();
                    subscribeToAG();
                } else if (clientInput.startsWith("sg")) {
                    num4 = 144;
                    freeBool2 = false;
                    dirPath = subscribedPath;
                    if (clientInput.length() > 3) {
                        num = Integer.parseInt(clientInput.substring(3));
                        subscribedAllGroups(true, num, dirPath);
                        counter = num;
                    } else if (clientInput.length() == 2) {
                        counter = 200;
                        num = 21;
                        subscribedAllGroups(false, 0, dirPath);
                    }
                } else if (clientInput.matches("rg \\S+")) {
                    counter = 200;
                    freeBool2 = true;
                    num4 = 1;
                    group = clientInput.substring(3);
                    allPosts();
                } else if (clientInput.matches("rg \\S+ \\d+")) {
                    freeBool2 = true;
                    num4 = 1;
                    counter = 0;
                    num = Integer.parseInt(clientInput.replaceAll("\\D+", ""));
                    num2 = num;
                    group = clientInput.substring(3).replaceAll("\\d*", "").replaceAll(" ", "");
                    getNPosts();
                } else if (clientInput.startsWith("u ")) {
                    loadNumList();
                    unsubscribe(numList);
                    sortSubscribed();
                    subscribeToAG();
                } else if (clientInput.equals("n")) {
                    if (num4 == 144) {
                        subscribedNGroups(dirPath);
                    } else if (!freeBool2) {
                        nGroups(dirPath);
                    } else {
                        getNPosts();
                    }
                } else if (clientInput.startsWith("r ")) {
                    if (clientInput.matches("r \\d+")) {
                        loadNumList();
                        moveToRead(false);
                    } else if (clientInput.matches("r \\d+-\\d+")) {
                        loadNumList();
                        moveToRead(true);
                    }
                } else if (clientInput.matches("\\d+")) {
                    loadCombinedPostList();
                    num3 = Integer.parseInt(clientInput);
                    sendPostContents();
                } else if (clientInput.equals("p")) {
                    createPost();
                } else if (clientInput.equals("logout")) {
                    clientSocket.close();
                    System.out.println("Client: " + clientNum + " disconnected");
                    return;
                }
            }
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static void loadNumList() throws IOException {
        numList = new ArrayList();
        clientInput = clientInput.replaceAll("\\D+", " "); //replace all nondecimals with a space
        scanner2 = new Scanner(clientInput);
        while (scanner2.hasNextInt()) {
            numList.add(scanner2.nextInt()); //add them to the arraylist
        }
        scanner2.close();
    }

    static void loadCombinedPostList() throws IOException {
        combinedPostList = new ArrayList<String>();
        Object[] dirs = Files.list(Paths.get(unreadPath)).toArray(); //combinedPostList stores the all the file names from unreadPath
        for (int i = 0; i < dirs.length; i++) {
            File f = new File(dirs[i].toString());
            combinedPostList.add(f.getName());
        }
        dirs = Files.list(Paths.get(readPath)).toArray(); //same but for read
        for (int i = 0; i < dirs.length; i++) {
            File f = new File(dirs[i].toString());
            combinedPostList.add(f.getName());
        }
    }

    static void loadPostList(String filePath) throws IOException { //load all the posts into this array, same method as befoer
        postList = new ArrayList();
        Object[] dirs = Files.list(Paths.get(filePath)).toArray();
        for (int i = 0; i < dirs.length; i++) {
            File f = new File(dirs[i].toString());
            postList.add(f.getName());
        }
    }

    static void sendPostContents() throws IOException { //send the contents of a  post
        if (num3 > combinedPostList.size()) {
            writeToClient("Cannot display post, index is out of bounds");
            return;
        }
        writeToClient("ok"); //ok protocol
        writeToClient(Integer.toString(getContentLength())); //the length of the post
        scanner = scanToFile(groupPostsPath + "/" + combinedPostList.get(num3 - 1));
        while (scanner.hasNextLine()) {
            String temp = scanner.nextLine();
            writeToClient(temp);
        }
        scanner.close();
        loadPostList(unreadPath);
        if (postList.contains(combinedPostList.get(num3 - 1))) {
            Files.copy(Paths.get(unreadPath + "/" + combinedPostList.get(num3 - 1)), Paths.get(readPath + "/" + combinedPostList.get(num3 - 1)));
            Files.delete(Paths.get(unreadPath + "/" + combinedPostList.get(num3 - 1)));
        }
    }

    static int getContentLength() throws IOException { //gets length of post.txt file, self explanatory
        int temp = 0;
        scanner = scanToFile(groupPostsPath + "/" + combinedPostList.get(num3 - 1));
        while (scanner.hasNextLine()) {
            scanner.nextLine();
            temp++;
        }
        scanner.close();
        return temp;
    }

    static void writeToClient(String input) throws IOException {
        toClient.write(input + '\n');
        toClient.flush();
    }

    static Boolean authenticateUser() throws IOException {
        user = clientInput.substring(6);
        subscribedPath = "test/login/" + user + "/subscribed.txt";
        tempPath = "test/login/" + user + "/temp.txt";
        SAGPath = "test/login/" + user + "/subscribedAG.txt";
        if (isUserExist(user)) {
            return true;
        }
        return false;
    }

    static void sortSubscribed() throws IOException {
        ArrayList listToSort = new ArrayList();
        File tempFile = new File(tempPath); //temp file
        File textFile = new File(subscribedPath); //subscribed.txt path,
        writer = writeToFile(tempFile.getCanonicalPath(), false);
        scanner = scanToFile(textFile.getCanonicalPath());
        clientInput = scanner.nextLine();
        while (scanner.hasNextLine()) {
            listToSort.add(scanner.nextLine().replace("-", "+")); //replace all subscribed - with a +
        }
        Collections.sort(listToSort, new Comparator<String>() { //this comparator sorts it in order
            public int compare(String o1, String o2) {
                return extractInt(o1) - extractInt(o2);
            }

            int extractInt(String s) {
                String num = s.replaceAll("\\D", "");
                return num.isEmpty() ? 0 : Integer.parseInt(num);
            }
        });
        writer.write(Integer.toString(listToSort.size()));
        writer.newLine();
        for (int i = 0; i < listToSort.size(); i++) {
            writer.write((String) listToSort.get(i));
            writer.newLine();
        }
        writer.close();
        scanner.close();
        updateFile(textFile, tempFile); //updateFile deletes textfile and replaces with tempfile and renames it
    }

    static void updateFile(File text, File temp) throws IOException { //follows from before
        if (text.delete()) {
            temp.renameTo(text);
        } else {
            System.err.println("DELETE FAILURE");
        }
    }

    static void sendUnread() throws IOException { //sends all unread posts
        temp = Integer.toString(getGroupPostSize(unreadPath));
        writeToClient(temp);
        for (int i = 0; i < Integer.parseInt(temp); i++) {
            scanner = scanToFile(unreadPath + "/" + postList.get(i));
            scanner.nextLine();
            String subject = scanner.nextLine();
            scanner.nextLine();
            String date = scanner.nextLine();
            scanner.nextLine();
            writeToClient(num + ". N " + date.substring(date.indexOf(",") + 1) + "   " + subject.substring(subject.indexOf(":") + 1));
            num++;
            scanner.close();
        }
    }

    static Boolean isUserExist(String user) throws IOException { //checks if user exists
        Object[] dirs = Files.list(Paths.get("test/login")).toArray();
        for (int i = 0; i < dirs.length; i++) {
            Path currentPath = (Path) dirs[i]; //uses path directories
            if (currentPath.getFileName().toString().compareTo(user) == 0) {
                writeToClient("Login successful!");
                return true;
            }
        }
        return false;
    }

    static Boolean isGroupExist(String group) throws IOException { //checks if group exists
        groupPostsPath = "test/groups/posts/" + group;
        unreadPath = "test/login/" + user + "/groups/" + group + "/unread";
        readPath = "test/login/" + user + "/groups/" + group + "/read";
        Object[] dirs = Files.list(Paths.get("test/groups/posts")).toArray();
        for (int i = 0; i < dirs.length; i++) { //same method
            Path currentPath = (Path) dirs[i];
            if (currentPath.getFileName().toString().compareTo(group) == 0) {
                return true;
            }
        }
        return false;
    }

    static int getGroupPostSize(String filePath) throws IOException { //gets the number of posts in a group
        postList = new ArrayList<String>();
        Object[] dirs = Files.list(Paths.get(filePath)).toArray();
        for (int i = 0; i < dirs.length; i++) {
            File f = new File(dirs[i].toString());
            postList.add(f.getName());
        }
        return dirs.length;
    }

    static void allPosts() throws IOException { // sends all posts
        num = 1;
        if (!isGroupExist(group)) {
            writeToClient("Cannot display posts, group does not exist");
            return;
        }
        writeToClient("ok");
        temp = Integer.toString(getGroupPostSize(unreadPath));
        writeToClient(temp);
        for (int i = 0; i < Integer.parseInt(temp); i++) {
            scanner = scanToFile(unreadPath + "/" + postList.get(i));
            scanner.nextLine();
            String subject = scanner.nextLine();
            scanner.nextLine();
            String date = scanner.nextLine();
            scanner.nextLine();
            writeToClient(num + ". N " + date.substring(date.indexOf(",") + 1) + "   " + subject.substring(subject.indexOf(":") + 1));
            num++;
            scanner.close();
        }
        temp = Integer.toString(getGroupPostSize(readPath));
        writeToClient(temp);
        for (int i = 0; i < Integer.parseInt(temp); i++) {
            scanner = scanToFile(readPath + "/" + postList.get(i));
            scanner.nextLine();
            String subject = scanner.nextLine();
            scanner.nextLine();
            String date = scanner.nextLine();
            scanner.nextLine();
            writeToClient(num + ".   " + date.substring(date.indexOf(",") + 1) + "   " + subject.substring(subject.indexOf(":") + 1));
            num++;
            scanner.close();
        }
    }

    static void createPost() throws IOException { //creates a new post with the information sent from the client
        writer = writeToFile(groupPostsPath + "/" + (getGroupPostSize(groupPostsPath) + 1) + ".txt", false);
        writer.write("Group: " + group);
        writer.newLine();
        clientInput = fromClient.readLine();
        writer.write("Subject: " + clientInput);
        writer.newLine();
        writer.write("Author: " + user);
        writer.newLine();
        writer.write("Date: " + String.format("%1$ta, %1$tb %1$te %1$tH:%1$tM:%1$tS EST %1$tY", LocalDateTime.now())); //the timme format
        writer.newLine();
        clientInput = fromClient.readLine();
        while (!clientInput.equals(".")) {
            writer.write(clientInput);
            writer.newLine();
            clientInput = fromClient.readLine();
        }
        writer.close();
        //copies into the unreadpath whenefver a new post in created
        Files.copy(Paths.get(groupPostsPath + "/" + (getGroupPostSize(groupPostsPath)) + ".txt"), Paths.get("test/login/bikong" + "/groups/" + group + "/unread" + "/" + getGroupPostSize(groupPostsPath) + ".txt"));
        Files.copy(Paths.get(groupPostsPath + "/" + (getGroupPostSize(groupPostsPath)) + ".txt"), Paths.get("test/login/xiaji" + "/groups/" + group + "/unread" + "/" + getGroupPostSize(groupPostsPath) + ".txt"));
        Files.copy(Paths.get(groupPostsPath + "/" + (getGroupPostSize(groupPostsPath)) + ".txt"), Paths.get("test/login/adandoune" + "/groups/" + group + "/unread" + "/" + getGroupPostSize(groupPostsPath) + ".txt"));
        allPosts();
    }

    static void moveToRead(boolean multiple) throws IOException { //move files to read folder
        if (numList.size() > 1) {
            if (numList.get(1) > getGroupPostSize(unreadPath)) {
                writeToClient("Cannot mark read, index out of bounds");
                return;
            }
        } else if (numList.get(0) > getGroupPostSize(unreadPath)) {
            writeToClient("Cannot mark read, index out of bounds");
            return;
        }
        num4 = getGroupPostSize(unreadPath);
        if (multiple) {
            for (int i = 0; i < num4; i++) {
                if (i >= (numList.get(0) - 1) && i <= (numList.get(1) - 1)) {
                    Files.copy(Paths.get(unreadPath + "/" + postList.get(i)), Paths.get(readPath + "/" + postList.get(i)));
                    Files.delete(Paths.get(unreadPath + "/" + postList.get(i)));
                }
            }
        } else {
            for (int i = 0; i < num4; i++) {
                if (i == (numList.get(0) - 1)) {
                    Files.copy(Paths.get(unreadPath + "/" + postList.get(i)), Paths.get(readPath + "/" + postList.get(i)));
                    Files.delete(Paths.get(unreadPath + "/" + postList.get(i)));
                }
            }
        }
        writeToClient("Read");
    }

    static void getNPosts() throws IOException { //gets theN posts
        if (!isGroupExist(group)) {
            writeToClient("Cannot display posts, group does not exist");
            return;
        }
        if (counter > 100) {
            writeToClient("Cannot list posts, all posts are displayed");
            return;
        }
        boolean triggered = false;
        if (num > getGroupPostSize(groupPostsPath)) {
            num = combinedPostList.size();
            triggered = true;
        }
        writeToClient("ok");
        loadCombinedPostList();
        if (triggered == false) {
            writeToClient(Integer.toString(num2));
        } else {
            writeToClient(Integer.toString(combinedPostList.size() - counter));
        }

        for (int i = counter; i < num; i++) {
            loadPostList(unreadPath);
            if (postList.contains(combinedPostList.get(i))) {
                scanner = scanToFile(unreadPath + "/" + combinedPostList.get(i));
                scanner.nextLine();
                String subject = scanner.nextLine();
                scanner.nextLine();
                String date = scanner.nextLine();
                scanner.nextLine();
                writeToClient((counter + 1) + ". N " + date.substring(date.indexOf(",") + 1) + "   " + subject.substring(subject.indexOf(":") + 1));
                counter++;
                scanner.close();
            } else {
                scanner = scanToFile(readPath + "/" + combinedPostList.get(i));
                scanner.nextLine();
                String subject = scanner.nextLine();
                scanner.nextLine();
                String date = scanner.nextLine();
                scanner.nextLine();
                writeToClient((counter + 1) + ".   " + date.substring(date.indexOf(",") + 1) + "   " + subject.substring(subject.indexOf(":") + 1));
                counter++;
                scanner.close();
            }
        }
        num += num2;
        if (triggered) {
            counter = 200;
        }
    }

    static void help() throws IOException {
        scanner = scanToFile("test/help.txt");
        while (scanner.hasNextLine()) {
            writeToClient(scanner.nextLine());
        }
        scanner.close();
    }

    static Scanner scanToFile(String filePath) throws IOException {
        scanner = null;
        try {
            scanner = new Scanner(new File(filePath));
        } catch (IOException e) {
            System.err.println("failed to open " + filePath);
            System.exit(1);
        }
        return scanner;
    }

    static BufferedWriter writeToFile(String filePath, Boolean append) throws IOException {
        writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filePath, append));
        } catch (IOException e) {
            System.err.println("failed to open " + filePath);
            System.exit(1);
        }
        return writer;
    }

    static void allGroups(Boolean hasNum, int num, String path) throws IOException {
        scanner = scanToFile(path);
        writeToClient(scanner.nextLine());
        if (hasNum.equals(true)) {
            for (int i = 0; i < (num); i++) {
                writeToClient(scanner.nextLine());
            }
        } else if (hasNum.equals(false)) {
            while (scanner.hasNextLine()) {
                writeToClient(scanner.nextLine());
            }
        }
        scanner.close();
    }

    static void subscribedAllGroups(Boolean hasNum, int num, String path) throws IOException {
        scanner = scanToFile(path);
        writeToClient(scanner.nextLine());
        if (hasNum.equals(true)) {
            for (int i = 0; i < (num); i++) {
                temp = scanner.nextLine();
                writeToClient(temp + " [" + getGroupPostSize("test/login/" + user + "/groups/" + temp.substring(7).replaceAll(" ", "") + "/unread") + "]");
            }
        } else if (hasNum.equals(false)) {
            while (scanner.hasNextLine()) {
                temp = scanner.nextLine();
                writeToClient(temp + " [" + getGroupPostSize("test/login/" + user + "/groups/" + temp.substring(7).replaceAll(" ", "") + "/unread") + "]");
            }
        }
        scanner.close();
    }

    static void nGroups(String path) throws IOException {
        boolean triggered = false;
        if (counter + num >= Integer.parseInt(getProtocol(path))) {
            triggered = true;
            num = (Integer.parseInt(getProtocol(path)) - counter);
        }
        if (counter > 100) {
            writeToClient("Cannot list groups, all groups displayed");
            return;
        } else {
            writeToClient("ok");
        }
        scanner = scanToFile(path);
        scanner.nextLine();
        for (int i = 0; i < counter; i++) {
            scanner.nextLine();
        }
        writeToClient(Integer.toString(num));
        for (int i = 0; i < num; i++) {
            writeToClient(scanner.nextLine());
        }
        scanner.close();
        if (counter + num < Integer.parseInt(getProtocol(path))) {
            counter += num;
        }
        if (triggered == true) {
            counter = 200;
        }
    }

    static void subscribedNGroups(String path) throws IOException {
        boolean triggered = false;
        if (counter + num >= Integer.parseInt(getProtocol(path))) {
            triggered = true;
            num = (Integer.parseInt(getProtocol(path)) - counter);
        }
        if (counter > 100) {
            writeToClient("Cannot list groups, all groups displayed");
            return;
        } else {
            writeToClient("ok");
        }
        scanner = scanToFile(path);
        scanner.nextLine();
        for (int i = 0; i < counter; i++) {
            scanner.nextLine();
        }
        writeToClient(Integer.toString(num));
        for (int i = 0; i < num; i++) {
            temp = scanner.nextLine();
            writeToClient(temp + " [" + getGroupPostSize("test/login/" + user + "/groups/" + temp.substring(7).replaceAll(" ", "") + "/unread") + "]");
        }
        scanner.close();
        if (counter + num < Integer.parseInt(getProtocol(path))) {
            counter += num;
        }
        if (triggered == true) {
            counter = 200;
        }
    }

    static boolean isSubscribed(ArrayList list, Boolean unsub) throws IOException {
        for (int i = 0; i < list.size(); i++) {
            scanner = scanToFile(subscribedPath);
            freeBool = false;
            while (scanner.hasNextLine()) {
                String temp = scanner.nextLine();
                if (temp.startsWith(list.get(i).toString() + '.')) {
                    if (!unsub) {
                        scanner.close();
                        return true;
                    } else {
                        freeBool = true;
                    }
                }
            }
            if (!freeBool) {
                if (unsub) {
                    scanner.close();
                    return false;
                }
            }
            scanner.close();
        }
        if (!unsub) {
            return false;
        } else if (unsub) {
            return true;
        }
        return true;
    }

    static boolean indexOutOfBounds(ArrayList list) throws IOException {
        for (int i = 0; i < list.size(); i++) {
            if ((int) list.get(i) > 20) {
                return true;
            }
        }
        return false;
    }

    static boolean duplicateIndex(ArrayList list) throws IOException {
        Set<Integer> set = new HashSet<Integer>(list);
        if (set.size() < list.size()) {
            return true;
        }
        return false;
    }

    static void subscribe(ArrayList nums) throws IOException {
        if (indexOutOfBounds(nums)) {
            writeToClient("Cannot subscribe, one or more indexes are out of bounds");
            return;
        }
        if (isSubscribed(nums, false)) {
            writeToClient("Cannot subscribe, one or more indexes are already subscribed");
            return;
        }
        if (duplicateIndex(nums)) {
            writeToClient("Cannot subscribe, one or more indexes occur more than once");
            return;
        }
        writer = writeToFile(subscribedPath, true);
        for (int i = 0; i < nums.size(); i++) {
            scanner = scanToFile(groupsPath);
            while (scanner.hasNextLine()) {
                temp = scanner.nextLine();
                if (temp.startsWith(nums.get(i).toString() + '.')) {
                    writer.write(temp);
                    writer.newLine();
                    break;
                }
            }
            scanner.close();
        }
        scanner.close();
        writer.close();
        writeToClient("Subscribe successful");
    }

    static void unsubscribe(ArrayList nums) throws IOException {
        File textFile = new File(subscribedPath);
        File tempFile = new File(tempPath);
        if (indexOutOfBounds(nums)) {
            writeToClient("Cannot unsubscribe, one or more indexes are out of bounds");
            return;
        }
        if (!isSubscribed(nums, true)) {
            writeToClient("Cannot unsubscribe, one or more indexes are not subscribed to");
            return;
        }
        if (duplicateIndex(nums)) {
            writeToClient("Cannot unsubscribe, one or more indexes occur more than once");
            return;
        }
        scanner = scanToFile(textFile.getCanonicalPath());
        writer = writeToFile(tempFile.getCanonicalPath(), false);
        while (scanner.hasNextLine()) {
            freeBool = true;
            temp = scanner.nextLine();
            for (int i = 0; i < nums.size(); i++) {
                if (temp.startsWith(nums.get(i).toString() + ".")) {
                    freeBool = false;
                }
            }
            if (freeBool) {
                writer.write(temp);
                writer.newLine();
            }
        }
        writer.close();
        scanner.close();
        updateFile(textFile, tempFile);
        writeToClient("Successfully unsubscribed!");
    }

    static void subscribeToAG() throws IOException {
        writer = writeToFile("test/login/" + user + "/subscribedAG.txt", false);
        writer.write("20");
        writer.newLine();
        scanner = scanToFile(groupsPath);
        temp = scanner.nextLine();
        while (scanner.hasNextLine()) {
            temp = scanner.nextLine();
            freeBool = true;
            scanner2 = new Scanner(new File("test/login/" + user + "/subscribed.txt"));
            clientInput = scanner2.nextLine();
            while (scanner2.hasNextLine()) {
                if (temp.startsWith(scanner2.nextLine().substring(0, 2))) {
                    temp = temp.replace("-", "+");
                    writer.write(temp);
                    writer.newLine();
                    freeBool = false;
                }
            }
            if (freeBool) {
                writer.write(temp);
                writer.newLine();
            }
            scanner2.close();
        }
        scanner.close();
        writer.close();
    }

    static String getProtocol(String filePath) throws IOException {
        scanner = scanToFile(filePath);
        int pro = Integer.parseInt(scanner.nextLine());
        scanner.close();
        return Integer.toString(pro);
    }
}
