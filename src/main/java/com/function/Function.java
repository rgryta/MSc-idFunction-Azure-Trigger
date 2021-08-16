package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
	
    @FunctionName("HttpsIDProvider")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.FUNCTION)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        Map<String,String> env = System.getenv();		
        try {
            JSONObject requestBody = new JSONObject(request.getBody());
    
            Connection connection = DriverManager.getConnection(env.get("URL"), env.get("USER"),env.get("PSWD"));
            context.getLogger().info("Database connection: " + connection.getCatalog());
    
            String stmt = "INSERT INTO DATA_ENTRIES (DATA_ENTRY) VALUES (?)";
            PreparedStatement statement = connection.prepareStatement(stmt);
            try{
                statement.setString(1,requestBody.optString("entry").toString());
                statement.executeUpdate();
            }
            catch(SQLException e){
                context.getLogger().warning(e.getMessage().toString());
            }
            context.getLogger().info("Logged: " + requestBody.optString("entry").toString());
            
            statement.close();
            connection.close();

            return request.createResponseBuilder(HttpStatus.OK).body("SUCCESS").build();
        }
        catch(Exception e){
            context.getLogger().warning(e.getMessage().toString());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(e.getMessage().toString()).build();
        }
    }
}
