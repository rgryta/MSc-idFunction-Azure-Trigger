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
        Supplier<String> descriptionSupplier = () -> "This is description for MovieItem";
        try {
            JSONObject requestBody = new JSONObject(request.getBody().orElseGet(descriptionSupplier).toString());
            String uncompressedStr = uncompressString(requestBody.optString("entry"));
            //JSONObject jsnMsg = new JSONObject(uncompressedStr);
    
            Connection connection = DriverManager.getConnection(env.get("URL"), env.get("USER"),env.get("PWD"));
            context.getLogger().info("Database connection: " + connection.getCatalog());
    
            String stmt = "INSERT INTO DATA_ENTRIES (DATA_ENTRY) VALUES (?)";
            PreparedStatement statement = connection.prepareStatement(stmt);
            try{
                statement.setString(1,uncompressedStr);
                statement.executeUpdate();
                statement.setString(1,requestBody.optString("entry"));
                statement.executeUpdate();
            }
            catch(SQLException e){
                context.getLogger().warning(e.getMessage().toString());
            }
            
            statement.close();
            connection.close();

            return request.createResponseBuilder(HttpStatus.OK).body("SUCCESS").build();
        }
        catch(Exception e){
            context.getLogger().warning(e.getMessage().toString());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(e.getMessage().toString()).build();
        }


        
    }
    public static String uncompressString(String dataToUncompress) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(dataToUncompress.getBytes(StandardCharsets.UTF_8));
        if (bytes == null || bytes.length == 0)
        {
            return null;
        }
        else
        {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            GZIPInputStream is = new GZIPInputStream(bais);
            byte[] tmp = new byte[256];
            while (true)
            {
                int r = is.read(tmp);
                if (r < 0)
                {
                    break;
                }
                buffer.write(tmp, 0, r);
            }
            is.close();

            byte[] content = buffer.toByteArray();
            return new String(content, StandardCharsets.UTF_8);
        }
    }
}
