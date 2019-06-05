
# Anypoint Template: Workday Worker to ServiceNow Service Request Broadcast

<!-- Header (start) -->
As worker information is added or removed from Workday, you may need to create service requests in ServiceNow. This template lets you broadcast (one way sync) those changes to workers in Workday to service requests in ServiceNow in real time. The detection criteria, and fields to move are configurable. Additional systems can easily added to be notified of changes. Real time synchronization is achieved via rapid polling of Workday or you can slow down the interval to something near real time.

![66a1d790-2819-46e9-8379-8a98316731e4-image.png](https://exchange2-file-upload-service-kprod.s3.us-east-1.amazonaws.com:443/66a1d790-2819-46e9-8379-8a98316731e4-image.png)

This template uses Mule batching and watermarking capabilities to ensure that only recent changes are captured and to efficiently process large amounts of records if you choose to slow down the polling interval.

## Workday Requirement

Install Workday HCM - Human Resources module, which you can find on the [Workday connector page](https://www.mulesoft.com/exchange/com.mulesoft.connectors/mule-workday-connector/).

<!-- Header (end) -->

# License Agreement
This template is subject to the conditions of the <a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>. Review the terms of the license before downloading and using this template. You can use this template for free with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.
# Use Case
<!-- Use Case (start) -->
When a new employee is hired, create two service requests:

1. Service request for setting up a desk.
2. Service request for setting up a computer.

If in the department, make the request for building A, if the department is anything else set their seat to building B.

The data is processed as follows:

1. Workday is polled in intervals for new modifications of employees.
2. Employee data is processed to identify new hires.
3. Service request for a computer is sent to ServiceNow.
4. Service request for a desk is sent to ServiceNow.
<!-- Use Case (end) -->

# Considerations
<!-- Default Considerations (start) -->

<!-- Default Considerations (end) -->

<!-- Considerations (start) -->
There are no special considerations for this template.
<!-- Considerations (end) -->

## ServiceNow Considerations

Here's what you need to know to get this template to work with ServiceNow.

### As a Data Destination

There are no considerations with using ServiceNow as a data destination.
## Workday Considerations

### As a Data Source

There are no considerations with using Workday as a data origin.

# Run it!
Simple steps to get this template running.
<!-- Run it (start) -->

<!-- Run it (end) -->

## Run On Premises

In this section we help you run this template on your computer.
<!-- Running on premise (start) -->

<!-- Running on premise (end) -->

### Download Anypoint Studio and the Mule Runtime
If you are new to Mule, download this software:

- [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
- [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)

**Note:** Anypoint Studio requires JDK 8.
<!-- Where to download (start) -->

<!-- Where to download (end) -->

### Import a Template into Studio

In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your Anypoint Platform credentials, search for the template, and click Open.
<!-- Importing into Studio (start) -->

<!-- Importing into Studio (end) -->

### Run on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

1. Locate the properties file `mule.dev.properties`, in src/main/resources.
2. Complete all the properties required per the examples in the "Properties to Configure" section.
3. Right click the template project folder.
4. Hover your mouse over `Run as`.
5. Click `Mule Application (configure)`.
6. Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`.
7. Click `Run`.

<!-- Running on Studio (start) -->

<!-- Running on Studio (end) -->

### Run on Mule Standalone
Update the properties in one of the property files, for example in mule.prod.properties, and run your app with a corresponding environment variable. In this example, use `mule.env=prod`.


## Run on CloudHub
When creating your application in CloudHub, go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the mule.env value.
<!-- Running on Cloudhub (start) -->

<!-- Running on Cloudhub (end) -->

### Deploy a Template in CloudHub
In Studio, right click your project name in Package Explorer and select Anypoint Platform > Deploy on CloudHub.
<!-- Deploying on Cloudhub (start) -->

<!-- Deploying on Cloudhub (end) -->

## Properties to Configure
To use this template, configure properties such as credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.

### Application Configuration

<!-- Application Configuration (start) -->
- scheduler.frequency `10000`
- scheduler.start.delay `500`
- watermark.default.expression `2018-08-27T10:08:00Z`

#### WorkDay Connector Configuration for Company A

- wday.username `user1`
- wday.tenant `mulesoft_pt1`
- wday.password `ExamplePassword565`
- wday.endpoint `https://services1.workday.com/ccx/service/acme/Human_Resources/v21.1`
- wday.department `sales`
- wday.responseTimeout `25000`

#### ServiceNow Connector Configuration for Company B

- snow.user `snow_user1`
- snow.password `ExamplePassword881`
- snow.endpoint `https://instance.service-now.com`

- snow.pc.assignedTo `1e826bf03710200044e0bfc8bcbe5d9c`
- snow.desk.assignedTo `1e826bf03710200044e0bfc8bcbe5d9c`
- snow.desk.model `5c7eb5092ba7a100c173448405da1563`
- snow.pc.model `e212a942c0a80165008313c59764eea1`
- snow.locationA `30fffb993790200044e0bfc8bcbe5dcc`
- snow.locationB `8228cda2ac1d55eb7029baf443945c37`

- snow.pc.deliveryDays `5`
- snow.pc.price `3000`

- snow.desk.deliveryDays `3`
- snow.desk.price` 500`
- snow.version `snow_version`
<!-- Application Configuration (end) -->

# API Calls
<!-- API Calls (start) -->
There are no special considerations regarding API calls.
<!-- API Calls (end) -->

# Customize It!
This brief guide provides a high level understanding of how this template is built and how you can change it according to your needs. As Mule applications are based on XML files, this page describes the XML files used with this template. More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

- config.xml
- businessLogic.xml
- endpoints.xml
- errorHandling.xml
<!-- Customize it (start) -->

<!-- Customize it (end) -->

## config.xml
<!-- Default Config XML (start) -->
This file provides the configuration for connectors and configuration properties. Only change this file to make core changes to the connector processing logic. Otherwise, all parameters that can be modified should instead be in a properties file, which is the recommended place to make changes.
<!-- Default Config XML (end) -->

<!-- Config XML (start) -->

<!-- Config XML (end) -->

## businessLogic.xml
<!-- Default Business Logic XML (start) -->
The business logic XML file creates or updates objects in the destination system for a represented use case. You can customize and extend the logic of this template in this XML file to more meet your needs.
<!-- Default Business Logic XML (end) -->

<!-- Business Logic XML (start) -->

<!-- Business Logic XML (end) -->

## endpoints.xml
<!-- Default Endpoints XML (start) -->
This file contains the endpoints for triggering the template and for retrieving the objects that meet the defined criteria in a query. You can execute a batch job process with the query results.
<!-- Default Endpoints XML (end) -->

<!-- Endpoints XML (start) -->

<!-- Endpoints XML (end) -->

## errorHandling.xml
<!-- Default Error Handling XML (start) -->
This file handles how your integration reacts depending on the different exceptions. This file provides error handling that is referenced by the main flow in the business logic.
<!-- Default Error Handling XML (end) -->

<!-- Error Handling XML (start) -->

<!-- Error Handling XML (end) -->

<!-- Extras (start) -->

<!-- Extras (end) -->
