/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package CSE310Source;

/**
 *
 * @author 晓程哥
 */
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import java.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

public class Server {

    private static BufferedWriter writer;
    private static BufferedReader fromClient;
    private static Scanner scanner;
    private static Scanner scanner2; //possibly uneeded
    private static String user;
    private static String clientInput;
    private static String temp;
    private static String group;
    private static boolean freeBool;
    private static final int serverPort = 47374;
    private static BufferedWriter toClient;
    private static int clientNum;
    private static int num;
    private static int counter;
    private static ArrayList<Integer> numList;
    private static ArrayList<String> postList;
    private static String subscribedPath;
    private static String groupsPath = "test/groups/allgroups.txt";
    private static String tempPath;
    private static String SAGPath;
    private static String dirPath;
    private static String unreadPath;
    private static String readPath;
    private static String groupPostsPath;

    public static void main(String[] args) {
        new Server();
    }
    private Socket clientSocket;
    private ServerSocket serverSocket;

    public Server() {
        //We need a try-catch because lots of errors can be thrown
        try {
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Server started at: " + new Date());
            System.out.println("Server listening on port " + serverPort);
            //Loop that runs server functions

            while (true) {
                try {//accept clients connecting with port #
                    //Wait for a client to connect
                    clientSocket = serverSocket.accept();
                    System.out.println("Client accepted");

                    fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    toClient = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                    ServerThread cT = new ServerThread(clientSocket);
                    new Thread(cT).start();
             //      commands();

                } catch (IOException e) {
                    System.err.println("Client failed to accept");
                    System.exit(1);
                }

            }
        } catch (IOException exception) {
            System.out.println("Error: " + exception);
            System.err.println("Could not listen on port " + serverPort);
            System.exit(1);
        }

    }

    void commands() throws IOException {

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
                dirPath = SAGPath;
                if (clientInput.length() > 3) {
                    num = Integer.parseInt(clientInput.substring(3));
                    allGroups(true, num, dirPath);
                    counter = num;
                } else if (clientInput.length() == 2) {
                    allGroups(false, 0, dirPath);
                }
            } else if (clientInput.startsWith("s ")) {
                loadNumList();
                subscribe(numList);
                sortSubscribed();
                subscribeToAG();
            } else if (clientInput.startsWith("sg")) {
                dirPath = subscribedPath;
                if (clientInput.length() > 3) {
                    num = Integer.parseInt(clientInput.substring(3));
                    allGroups(true, num, dirPath);
                    counter = num;
                } else if (clientInput.length() == 2) {
                    allGroups(false, 0, dirPath);
                }
            } else if (clientInput.startsWith("rg ")) {
                group = clientInput.substring(3);
                allPosts();
            } else if (clientInput.startsWith("u ")) {
                loadNumList();
                unsubscribe(numList);
                sortSubscribed();
                subscribeToAG();
            } else if (clientInput.equals("n")) {
                nGroups(dirPath);
            } else if (clientInput.startsWith("r ")) {
                if (clientInput.matches("r \\d+")) {
                    loadNumList();
                    moveToRead(false);
                } else if (clientInput.matches("r \\d+-\\d+")) {
                    loadNumList();
                    moveToRead(true);
                }
            } else if (clientInput.equals("p")) {
                createPost();
            } else if (clientInput.equals("logout")) {
                clientSocket.close();
                System.out.println("Client: " + clientNum + " disconnected");
                return;
            }
        }

    }

    static void loadNumList() throws IOException {
        numList = new ArrayList();
        clientInput = clientInput.replaceAll("\\D+", " ");
        scanner2 = new Scanner(clientInput);
        while (scanner2.hasNextInt()) {
            numList.add(scanner2.nextInt());
        }
        scanner2.close();
        for (int i = 0; i < numList.size(); i++) {
            System.out.println(numList.get(i));
        }
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
        File tempFile = new File(tempPath);
        File textFile = new File(subscribedPath);
        writer = writeToFile(tempFile.getCanonicalPath(), false);
        scanner = scanToFile(textFile.getCanonicalPath());
        clientInput = scanner.nextLine();
        while (scanner.hasNextLine()) {
            listToSort.add(scanner.nextLine().replace("-", "+"));
        }
        Collections.sort(listToSort, new Comparator<String>() {
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
        updateFile(textFile, tempFile);
    }

    static void updateFile(File text, File temp) throws IOException {
        if (text.delete()) {
            temp.renameTo(text);
        } else {
            System.err.println("DELETE FAILURE");
        }
    }

    static Boolean isUserExist(String user) throws IOException {
        Object[] dirs = Files.list(Paths.get("test/login")).toArray();
        for (int i = 0; i < dirs.length; i++) {
            Path currentPath = (Path) dirs[i];
            if (currentPath.getFileName().toString().compareTo(user) == 0) {
                writeToClient("Login successful!");
                return true;
            }
        }
        return false;
    }

    static Boolean isGroupExist(String group) throws IOException {
        System.out.println("group is " + group);
        groupPostsPath = "test/groups/posts/" + group;
        unreadPath = "test/login/" + user + "/groups/" + group + "/unread";
        readPath = "test/login/" + user + "/groups/" + group + "/read";
        Object[] dirs = Files.list(Paths.get("test/groups/posts")).toArray();
        for (int i = 0; i < dirs.length; i++) {
            Path currentPath = (Path) dirs[i];
            if (currentPath.getFileName().toString().compareTo(group) == 0) {
                System.out.println(currentPath.getFileName());
                return true;
            }
        }
        return false;
    }

    static int getGroupPostSize(String filePath) throws IOException {
        postList = new ArrayList<String>();
        Object[] dirs = Files.list(Paths.get(filePath)).toArray();
        for (int i = 0; i < dirs.length; i++) {
            File f = new File(dirs[i].toString());
            postList.add(f.getName());
        }
        return dirs.length;
    }

    static void allPosts() throws IOException {
        num = 1;
        if (!isGroupExist(group)) {
            System.out.println("test?");
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

    static void createPost() throws IOException {
        writer = writeToFile(groupPostsPath + "/" + (getGroupPostSize(groupPostsPath) + 1) + ".txt", false);
        writer.write("Group: " + group);
        writer.newLine();
        clientInput = fromClient.readLine();
        writer.write("Subject: " + clientInput);
        writer.newLine();
        writer.write("Author: " + user);
        writer.newLine();
        writer.write("Date: " + String.format("%1$ta, %1$tb %1$te %1$tH:%1$tM:%1$tS EST %1$tY", LocalDateTime.now()));
        writer.newLine();
        clientInput = fromClient.readLine();
        while (!clientInput.equals(".")) {
            writer.write(clientInput);
            writer.newLine();
            clientInput = fromClient.readLine();
        }
        writer.close();

        Files.copy(Paths.get(groupPostsPath + "/" + (getGroupPostSize(groupPostsPath)) + ".txt"), Paths.get("test/login/bikong" + "/groups/" + group + "/unread" + "/" + getGroupPostSize(groupPostsPath) + ".txt"));
        Files.copy(Paths.get(groupPostsPath + "/" + (getGroupPostSize(groupPostsPath)) + ".txt"), Paths.get("test/login/xiaji" + "/groups/" + group + "/unread" + "/" + getGroupPostSize(groupPostsPath) + ".txt"));
        Files.copy(Paths.get(groupPostsPath + "/" + (getGroupPostSize(groupPostsPath)) + ".txt"), Paths.get("test/login/adandoune" + "/groups/" + group + "/unread" + "/" + getGroupPostSize(groupPostsPath) + ".txt"));
    }

    static void moveToRead(boolean multiple) throws IOException {
        if (numList.size() > 1) {
            if (numList.get(1) > getGroupPostSize(unreadPath)) {
                writeToClient("Cannot mark read, index out of bounds");
                return;
            }
        } else if (numList.get(0) > getGroupPostSize(unreadPath)) {
            writeToClient("Cannot mark read, index out of bounds");
            return;
        }
        num = getGroupPostSize(unreadPath);
        if (multiple) {
            for (int i = 0; i < num; i++) {
                if (i >= (numList.get(0) - 1) && i <= (numList.get(1) - 1)) {
                    Files.copy(Paths.get(unreadPath + "/" + postList.get(i)), Paths.get(readPath + "/" + postList.get(i)));
                    Files.delete(Paths.get(unreadPath + "/" + postList.get(i)));
                }
            }
        } else {
            for (int i = 0; i < num; i++) {
                if (i == (numList.get(0) - 1)) {
                    Files.copy(Paths.get(unreadPath + "/" + postList.get(i)), Paths.get(readPath + "/" + postList.get(i)));
                    Files.delete(Paths.get(unreadPath + "/" + postList.get(i)));
                }
            }
        }
        writeToClient("Read");
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
        if (hasNum.equals(true)) {
            for (int i = 0; i < (num) + 1; i++) {
                writeToClient(scanner.nextLine());
            }
        } else if (hasNum.equals(false)) {
            while (scanner.hasNextLine()) {
                writeToClient(scanner.nextLine());
            }
        }
        scanner.close();
    }

    static void nGroups(String path) throws IOException {
        if (counter + num > Integer.parseInt(getProtocol(path))) {
            writeToClient("The next N groups is out of bounds");
            return;
        } else {
            writeToClient("ok");
        }
        scanner = scanToFile(path);
        scanner.nextLine();
        for (int i = 0; i < counter; i++) {
            scanner.nextLine();
        }
        for (int i = 0; i < num; i++) {
            writeToClient(scanner.nextLine());
        }
        scanner.close();
        counter += num;
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
        System.out.println("set size = " + set.size());
        System.out.println("list size = " + list.size());

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
            writeToClient("Unsubscribe unsuccessfull, one or more indexes are out of bounds");
            return;
        }
        if (!isSubscribed(nums, true)) {
            writeToClient("Unsubscribe unsuccessfull, one or more indexes are not subscribed to");
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

    //Here we create the ClientThread inner class and have it implement Runnable
    //This means that it can be used as a thread
}
