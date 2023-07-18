package com.example.dataDictionary;
import io.r2dbc.spi.ColumnMetadata;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class databaseController {

    @PostMapping("/upload")
    public String handleFormSubmission(@RequestParam("username") String username,
                                       @RequestParam("password") String password,
                                       @RequestParam("server") String server,
                                       @RequestParam("port") String port,
                                       @RequestParam("database") String database,
                                       Model model) {

        String dbUrl = "jdbc:mysql://" + server + ":" + port + "/" + database;

        try (Connection connection = DriverManager.getConnection(dbUrl, username, password)) {
            String selectQuery = "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, COLUMN_KEY, IS_NULLABLE, COLUMN_DEFAULT " +
                    "FROM information_schema.columns WHERE TABLE_SCHEMA = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
                preparedStatement.setString(1, database);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    Map<String, List<String[]>> tableDataMap = new HashMap<>();

                    while (resultSet.next()) {
                        String tableName = resultSet.getString("TABLE_NAME");
                        String columnName = resultSet.getString("COLUMN_NAME");
                        String dataType = resultSet.getString("DATA_TYPE");
                        String columnType = resultSet.getString("COLUMN_TYPE");
                        String columnKey = resultSet.getString("COLUMN_KEY");
                        String isNullable = resultSet.getString("IS_NULLABLE");
                        String columnDefault = resultSet.getString("COLUMN_DEFAULT");

                        String[] rowData = { columnName, dataType, isNullable, columnDefault, columnType };

                        tableDataMap.computeIfAbsent(tableName, k -> new ArrayList<>()).add(rowData);
                    }

                    String[][][] tableData = new String[tableDataMap.size()][][];

                    int i = 0;
                    for (String tableName : tableDataMap.keySet()) {
                        List<String[]> rowDataList = tableDataMap.get(tableName);
                        int numRows = rowDataList.size() + 2;
                        int numCols = rowDataList.get(0).length;

                        tableData[i] = new String[numRows][numCols];

                        tableData[i][0][0] = tableName;

                        String[] headers = { "COLUMN", "TYPE", "NULL", "DEFAULT", "COMMENT" };
                        tableData[i][1] = headers;

                        for (int j = 0; j < rowDataList.size(); j++) {
                            tableData[i][j + 2] = rowDataList.get(j);
                        }

                        i++;
                    }

                    createDocx.createDocument(tableData);
                }
            }

            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "error";
    }


}
