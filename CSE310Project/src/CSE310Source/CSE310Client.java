package CSE310Source;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class CSE310Client {

    private static BufferedReader bufferedReader;
    private static BufferedReader fromServer;
    private static Socket socket;
    private static BufferedWriter toServer;
    private static String host;
    private static int port;
    private static String serverInput;
    private static String inputLine;
    private static int num;
    private static int counter;

    public static void main(String args[]) throws IOException {
        if (args.length != 2) {
            System.out.println("Invalid arguments");
            System.exit(0);
        }
        bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        host = args[0];
        port = Integer.parseInt(args[1]);
        socket = null;
        try {
            socket = new Socket(host, port);
            System.out.println("Server reached");
            toServer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Failed to connect to server");
            System.exit(1);
        }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Boolean loggedIn = false;
        System.out.println("To login, please type \"login\" followed by your user ID");
        while (!loggedIn) {
            System.out.print('>');
            inputLine = bufferedReader.readLine();
            if (inputLine.matches("login .*")) {
                writeToServer(inputLine);
                serverInput = fromServer.readLine();
                if (serverInput.equals("Login successful!")) {
                    loggedIn = true;
                } else {
                    System.out.println(serverInput);
                }
            } else {
                System.out.println("Invalid command, please try again");
            }
        }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        System.out.println(serverInput + " For a list of commands type \"help\"");
        while (loggedIn) {
            System.out.print('>');
            inputLine = bufferedReader.readLine();
            if (inputLine.startsWith("login")) {
                System.out.println("You are already logged in. For a list of commands type \"help\"");
            } else if (inputLine.equals("help")) {
                getHelp();
            } else if (inputLine.startsWith("ag")) {
                if (inputLine.equals("ag")) {
                    getGroups(false, 0, "ag");
                } else if (inputLine.matches("ag \\d+")) {
                    writeToServer("protocol");
                    if (Integer.parseInt(inputLine.substring(3)) <= topProtocol()) {
                        num = Integer.parseInt((inputLine.substring(3)));
                        getGroups(true, num, "ag");
                    } else {
                        System.out.println("Index out of bounds");
                    }
                } else {
                    System.out.println("Invalid command. For a list of commands type \"help\"");
                }
            } else if (inputLine.startsWith("sg")) {
                if (inputLine.equals("sg")) {
                    getGroups(false, 0, "sg");
                } else if (inputLine.matches("sg \\d+")) {
                    writeToServer("protocol2");
                    if (Integer.parseInt(inputLine.substring(3)) <= topProtocol()) {
                        num = Integer.parseInt((inputLine.substring(3)));
                        getGroups(true, num, "sg");
                    } else {
                        System.out.println("Index out of bounds");
                    }
                } else {
                    System.out.println("Invalid command. For a list of commands type \"help\"");
                }
            } else if (inputLine.startsWith("rg")) {
                if (inputLine.matches("rg \\S+")) {
                    writeToServer(inputLine);
                    serverInput = fromServer.readLine();
                    if (serverInput.equals("ok")) {
                        getGroups(false, 0, "rg");
                    } else {
                        System.out.println(serverInput);
                    }
                } else if (inputLine.matches("rg \\S+ \\d+")) {
                    num = Integer.parseInt(inputLine.replaceAll("\\D+", ""));
                    writeToServer(inputLine);
                    serverInput = fromServer.readLine();
                    if (serverInput.equals("ok")) {
                        getGroups(true, num, "rg");
                    } else {
                        System.out.println(serverInput);
                    }
                } else {
                    System.out.println("Invalid command. For a list of commands type \"help\"");
                }
            } else if (inputLine.equals("logout")) {
                writeToServer(inputLine);
                socket.close();
                System.out.println("You are now logged out");
                loggedIn = false;
                return;
            } else {
                System.out.println("Invalid command. For a list of commands type \"help\"");
            }
        }
    }

    static void subRoutine(String route) throws IOException {
        boolean tick = true;
        while (tick == true) {
            System.out.print('>');
            inputLine = bufferedReader.readLine();
            if (route.equals("ag") || route.equals("sg")) {
                if (inputLine.matches("s(?: \\d+)+") && route.equals("ag")) {
                    writeToServer(inputLine);
                    printResponse();
                } else if (inputLine.matches("u(?: \\d+)+")) {
                    writeToServer(inputLine);
                    printResponse();
                } else if (inputLine.equals("n")) {
                    writeToServer(inputLine);
                    getNGroups();
                    if (serverInput.contains("Cannot")) {
                        System.out.println("Exiting " + route);
                        return;
                    }
                } else if (inputLine.equals("q")) {
                    tick = false;
                    return;
                } else if (inputLine.equals("help")) {
                    writeToServer(inputLine);
                    getHelp();
                } else {
                    System.out.println("Invalid command. For a list of commands type \"help\"");
                }
            } else if (route.contains("rg")) {
                if (inputLine.matches("r \\d+")) {
                    writeToServer(inputLine);
                    printResponse();
                } else if (inputLine.matches("r \\d+-\\d+")) {
                    writeToServer(inputLine);
                    printResponse();
                } else if (inputLine.matches("\\d+")) {
                    writeToServer(inputLine);
                    getPostContents();
                } else if (inputLine.equals("n")) {
                    writeToServer(inputLine);
                    getNGroups();
                    if (serverInput.contains("Cannot")) {
                        System.out.println("Exiting " + route);
                        return;
                    }
                } else if (inputLine.equals("p")) {
                    setUpNewPost();
                } else if (inputLine.equals("q")) {
                    tick = false;
                    return;
                } else {
                    System.out.println("Invalid command. For a list of commands type \"help\"");
                }
            }
        }
    }

    static void printResponse() throws IOException {
        serverInput = fromServer.readLine();
        System.out.println(serverInput);
    }

    static void setUpNewPost() throws IOException {
        writeToServer(inputLine);
        System.out.print("Subject: ");
        inputLine = bufferedReader.readLine();
        writeToServer(inputLine);
        System.out.println("Enter for new line, end post with \".\"");
        while (!inputLine.equals(".")) {
            inputLine = bufferedReader.readLine();
            writeToServer(inputLine);
        }
        serverInput = fromServer.readLine();
        if (serverInput.equals("ok")) {
            getGroups(false, 0, "nonpersistant");
        }

    }

    static void getHelp() throws IOException {
        writeToServer(inputLine);
        counter = topProtocol();
        for (int i = 0; i < counter; i++) {
            printResponse();
        }
    }

    static void getGroups(boolean hasNum, int num, String subroute) throws IOException {
        if (!subroute.equals("rg")) {
            writeToServer(inputLine);
        }
        if (num == 0 || subroute.equals("ag") || subroute.equals("sg")) {
            counter = topProtocol();
        }
        if (hasNum == false) {
            for (int i = 0; i < counter; i++) {
                printResponse();
            }
            if (subroute.equals("rg") || subroute.equals("nonpersistant")) {
                counter = topProtocol();
                for (int i = 0; i < counter; i++) {
                    printResponse();
                }
            }
        } else if (hasNum == true) {
            if (subroute.equals("rg")) {
                num = topProtocol();
            }
            for (int i = 0; i < num; i++) {
                printResponse();
            }
        }
        if (!subroute.equals("nonpersistant")) {
            subRoutine(subroute);
        }
    }

    static void getNGroups() throws IOException {
        serverInput = fromServer.readLine();
        if (serverInput.equals("ok")) {
            num = Integer.parseInt(fromServer.readLine());
            for (int i = 0; i < num; i++) {
                printResponse();
            }
        } else {
            System.out.println(serverInput);
        }
    }

    static void getPostContents() throws IOException {
        serverInput = fromServer.readLine();
        if (serverInput.equals("ok")) {
            counter = topProtocol();
            for (int i = 0; i < counter; i++) {
                printResponse();
            }
        } else {
            System.out.println(serverInput);
            return;
        }
    }

    static int topProtocol() throws IOException {
        return Integer.parseInt(fromServer.readLine());
    }

    static void writeToServer(String input) throws IOException {
        toServer.write(input + '\n');
        toServer.flush();
    }
}
