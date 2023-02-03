package com.revature.controllers;

import com.revature.model.Employee;
import com.revature.service.EmployeeService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import org.codehaus.jackson.map.ObjectMapper;


/**
 * @author Treyvon Whitaker
 *         <p>
 *         This class uses {@link HttpHandler} to create a context for our
 *         backend server that allows employees to login using an account that
 *         is in the the database.
 *         </p>
 *         See Also:
 *         <ul>
 *         <li>{@link EmployeeRegister}</li>
 *         <li>{@link ManagerLogin}</li>
 *         <li>{@link ManagerRegister}</li>
 *         </ul>
 *         for more information on other contexts.
 */
public class EmployeeLogin implements HttpHandler {

    // Global variable for the Return Codes
    private static final int RCODE_SUCCESSFUL = 200;
    private static final int RCODE_REDIRECT = 302;
    private static final int RCODE_CLIENT_ERROR = 400;

    private static final String badEmail = "BadEmail";
    private static final String badPass = "BadPassword";

    private static final String ticketRedirect = "http://localhost:8000/submitTicket";

    // Add Doc
    private void getRequest(HttpExchange exchange) {
        try {
            File file = new File("ersproject/src/main/java/com/revature/view/employeeLogin.html"); 
            OutputStream os = exchange.getResponseBody();
            exchange.sendResponseHeaders(RCODE_SUCCESSFUL, file.length());
            Files.copy(file.toPath(), os);
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * <p>
     * This method processes a <code>POST</code> request from the client to login
     * using an existing account.
     * </p>
     * 
     * @param exchange the exchange captured by the server
     */
    private void postRequest(HttpExchange exchange) {
        // Read in login information from client
        InputStream is = exchange.getRequestBody();
        StringBuilder textBuilder = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        try (Reader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName(StandardCharsets.UTF_8.name())))) {

            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        
            String test = textBuilder.toString();
            // Convert client login from json to Employee object
            Employee newEmployee = mapper.readValue(test, Employee.class);

            // Get all Employees currently in database
            EmployeeService service = new EmployeeService();
            List<Employee> listEmployees = service.getAllObjects();

            // Search if client login email is in database
            // If it is send a Back Request RCODE back to client
            List<String> listEmails = service.getAllColumnString("email");
    
            int index = Collections.binarySearch(listEmails, newEmployee.getEmail());

            // Check if password match to what is in database
            // Send OK response back to client
            OutputStream os = exchange.getResponseBody();
            String response;
            if (index < 0) {
                response = badEmail;
                exchange.sendResponseHeaders(RCODE_CLIENT_ERROR, response.getBytes().length);
                os.write(response.getBytes());
            } else {
                if ((listEmployees.get(index).getPassword()).equals(newEmployee.getPassword())) {
                    exchange.getResponseHeaders().add("Location", ticketRedirect);
                    exchange.sendResponseHeaders(RCODE_REDIRECT, -1);
                } else {
                    response = badPass;                    
                    exchange.sendResponseHeaders(RCODE_CLIENT_ERROR, response.getBytes().length);
                    os.write(response.getBytes());
                }
            }
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>
     * This method handles processing the "verbs" from the client.
     * </p>
     * 
     * @param exchange the exchange captured by the server
     */
    @Override
    public void handle(HttpExchange exchange) {
        String verb = exchange.getRequestMethod();

        switch (verb) {
            case "GET":
                getRequest(exchange);
                break;
            case "POST":
                postRequest(exchange);
                break;
            default:
                break;
        }
    }
}
