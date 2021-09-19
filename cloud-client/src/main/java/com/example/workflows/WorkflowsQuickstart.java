
package com.example.workflows;

// [START workflows_api_quickstart]

// Imports the Google Cloud client library

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.workflows.executions.v1.CreateExecutionRequest;
import com.google.cloud.workflows.executions.v1.Execution;
import com.google.cloud.workflows.executions.v1.ExecutionsClient;
import com.google.cloud.workflows.executions.v1.WorkflowName;


import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class WorkflowsQuickstart {

  //private static final String PROJECT = System.getenv("GOOGLE_CLOUD_PROJECT");
  private static final String LOCATION = System.getenv().getOrDefault("LOCATION", "us-central1");
  private static final String WORKFLOW = System.getenv().getOrDefault("WORKFLOW",
      "request-workflow");

  public static void main(String... args)
      throws IOException, InterruptedException, ExecutionException {

    workflowsQuickstart("projectId", LOCATION, WORKFLOW);
  }

  private static volatile boolean finished;

  public static void workflowsQuickstart(String projectId, String location, String workflow)
      throws IOException, InterruptedException, ExecutionException {


    // Initialize client that will be used to send requests. This client only needs
    // to be created once, and can be reused for multiple requests. After completing all of your
    // requests, call the "close" method on the client to safely clean up any remaining background
    // resources.
    try (ExecutionsClient executionsClient = ExecutionsClient.create()) {

      // Construct the fully qualified location path.
      WorkflowName parent = WorkflowName.of(projectId, location, workflow);

      WorkflowArgument workflowArgument=new WorkflowArgument();
      workflowArgument.setProjectname("PROJECTNAME");
      workflowArgument.setTopicname("TOPICNAME");
      workflowArgument.setData("SGVsbG8gV29ybGQ=");

      ObjectMapper Obj = new ObjectMapper();

        // Converting the Java object into a JSON string
        String jsonStr = Obj.writeValueAsString(workflowArgument);
        // Creates the execution object.
      CreateExecutionRequest request =
          CreateExecutionRequest.newBuilder()
              .setParent(parent.toString())
              .setExecution(Execution.newBuilder().setArgument(jsonStr).build())
              .build();
      Execution response = executionsClient.createExecution(request);

      String executionName = response.getName();
      System.out.printf("Created execution: %s%n", executionName);

      long backoffTime = 0;
      long backoffDelay = 1_000; // Start wait with delay of 1,000 ms
      final long backoffTimeout = 10 * 60 * 1_000; // Time out at 10 minutes
      System.out.println("Poll for results...");
      
      // Wait for execution to finish, then print results.
      while (!finished && backoffTime < backoffTimeout) {
        Execution execution = executionsClient.getExecution(executionName);
        finished = execution.getState() != Execution.State.ACTIVE;

        // If we haven't seen the results yet, wait.
        if (!finished) {
          System.out.println("- Waiting for results");
          Thread.sleep(backoffDelay);
          backoffTime += backoffDelay;
          backoffDelay *= 2; // Double the delay to provide exponential backoff.
        } else {
          System.out.println("Execution finished with state: " + execution.getState().name());
          System.out.println("Execution results: " + execution.getResult());
        }
      }
    }
  }
}
// [END workflows_api_quickstart]