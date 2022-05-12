#Author: Bharath.KM@mnscorp.net
#Feature: POST service to add the PAL Project
#StoryDescription : POST service to add the PAL Project
#Adding validation to check the response whether the project has been creating correctly
@addPALProject


Feature: Post addPALProject Functionality


  @addPALProject_positive
  Scenario Outline: Positive test to create project by passing valid mandatory fields
    When the service call for PAL Project  with POST request with "/v1/pal/PALProject/addProject" with payload with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost"
     """
  {
	"information": {
		"projectName":<projectName> ,
		"projectType": <projectType>,
		"financialYear": <financialYear>,
		"projectCompletionDate": <projectCompletionDate>,
		"comments":<comments>
	},
	"personnel": {
		"internal": [
			{
				"role": <personnelrole1>,
				"users":[ <personneluser1>]
			},
			{
				"role": <personnelrole2>,
				"users": [<personneluser2>]
			},
			{
				"role": <personnelrole3>,
				"users":[<personneluser3>]
			},
			{
				"role": <personnelrole4>,
				"users": [<personneluser4>]
			}
		]
	},
		"userRole": <userrole>,
		"user": <user>

}

  """
    Then service should return 201 status code
    Then response must match with json schema "schema/addPALProject.json".
    Then assert the response for valid values
    Examples:
      |projectName          |projectType   |financialYear|projectCompletionDate     |comments         |personnelrole1  |personneluser1                           |personnelrole2    |personneluser2                        |personnelrole3    |personneluser3                       |personnelrole4|personneluser4                        |userrole        |user         |
      |"Christmas Project"  |"Rolling"     |"2021-2022"  |"2022-01-27T00:00:00.000Z"|   null          |"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|
      |"Christmas Project"  |"Rolling"     |"2021-2022"  |     null                 |   null          |"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|
      |"Christmas Project"  |"Standard NPD"|"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|"Dileep.Gireesh@marks-and-spencer.com"|"foodTechnologist"|"Adarsh.Shenoy@marks-and-spencer.com"|"buyer"       |"Shubham.Prasad@marks-and-spencer.com"|"projectManager"|"KM, Bharath"|
      |"Christmas Project"  |"Rolling"     |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|
      |"Christmas Project"  |"Rolling"     |"2021-2022"  |"2022-01-27T00:00:00.000Z"|   null          |"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|"Dileep.Gireesh@marks-and-spencer.com"|"foodTechnologist"|"Adarsh.Shenoy@marks-and-spencer.com"|"buyer"       |"Shubham.Prasad@marks-and-spencer.com"|"projectManager"|"KM, Bharath"|

  @addPALProject_positive_positive
  Scenario Outline: Positive test to create project by passing valid mandatory fields
    When the service call for PAL Project  with POST request with "/v1/pal/PALProject/addProject" with payload with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost"
     """
  {
	"information": {
		"projectName":<projectName> ,
		"projectType": <projectType>,
		"financialYear": <financialYear>,
		"projectCompletionDate": <projectCompletionDate>,
		"comments":<comments>
	},
	"personnel": {
		"internal": [
			{
				"role": <personnelrole1>,
				"users":[ <personneluser1>]
			},
			{
				"role": <personnelrole2>,
				"users": [<personneluser2>]
			},
			{
				"role": <personnelrole3>,
				"users":[<personneluser3>]
			},
			{
				"role": <personnelrole4>,
				"users": [<personneluser4>]
			}
		]
	},
		"userRole": <userrole>,
		"user": <user>

}

  """
    Then service should return 201 status code
    Then response must match with json schema "schema/addPALProject.json".
    Then assert the response for valid values
    Examples:
      |projectName        |projectType|financialYear|projectCompletionDate     |comments         |personnelrole1  |personneluser1                           |personnelrole2    |personneluser2                        |personnelrole3    |personneluser3                       |personnelrole4|personneluser4                        |userrole        |user         |
      |"Christmas Project"|"Rolling"  |"invalid"    |     ""                   |"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|
      |      "@#$"        |"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"invalid"         |      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"invalid"         |       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"invalid"     |        ""                            |"projectManager"|"KM, Bharath"|


  @addPALProject_invalidmandatoryfield
  Scenario Outline: Negative test to create project by passing invalid mandatory fields
    When the service call for PAL Project  with POST request with "/v1/pal/PALProject/addProject" for negative scenarios with payload with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost"
      """
  {
	"information": {
		"projectName":<projectName> ,
		"projectType": <projectType>,
		"financialYear": <financialYear>,
		"projectCompletionDate": <projectCompletionDate>,
		"comments":<comments>
	},
	"personnel": {
		"internal": [
			{
				"role": <personnelrole1>,
				"users":[ <personneluser1>]
			},
			{
				"role": <personnelrole2>,
				"users": [<personneluser2>]
			},
			{
				"role": <personnelrole3>,
				"users":[<personneluser3>]
			},
			{
				"role": <personnelrole4>,
				"users": [<personneluser4>]
			}
		]
	},
		"userRole": <userrole>,
		"user": <user>

}

  """
    Then service should return 400 status code
    Then cross verify with the response if the error message is "<errorMessage>".
    Then response must match with json schema "schema/errorResponse.json".
    Examples:
      |projectName        |projectType   |financialYear|projectCompletionDate     |comments         |personnelrole1  |personneluser1                           |personnelrole2    |personneluser2                        |personnelrole3    |personneluser3                       |personnelrole4|personneluser4                        |userrole        |user         |errorMessage|
      |"Christmas Project"|"Rolling"     |     ""      |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|Missing mandatory fields|
      |"Christmas Project"|""            |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|"Dileep.Gireesh@marks-and-spencer.com"|"foodTechnologist"|"Adarsh.Shenoy@marks-and-spencer.com"|"buyer"       |"Shubham.Prasad@marks-and-spencer.com"|"projectManager"|"KM, Bharath"|Missing mandatory fields|
      |"Christmas Project"|"invalid"     |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|Bad Request             |
      |       ""          |"Rolling"     |"2021-2022"  |"2022-01-27T00:00:00.000Z"|   null          |"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|"Dileep.Gireesh@marks-and-spencer.com"|"foodTechnologist"|"Adarsh.Shenoy@marks-and-spencer.com"|"buyer"       |"Shubham.Prasad@marks-and-spencer.com"|"projectManager"|"KM, Bharath"|Missing mandatory fields|
      |"Christmas Project"|"Rolling"     |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|     ""         |"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|Bad Request             |
      |"Christmas Project"|"Standard NPD"|"2021-2022"  |  ""                      |"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|Missing mandatory fields|

  @addPALProject_internaluserrole
  Scenario Outline: Negative test to create project by passing various internal userroles
    When the service call for PAL Project  with POST request with "/v1/pal/PALProject/addProject" for negative scenarios with payload with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost"
     """
  {
	"information": {
		"projectName":<projectName> ,
		"projectType": <projectType>,
		"financialYear": <financialYear>,
		"projectCompletionDate": <projectCompletionDate>,
		"comments":<comments>
	},
	"personnel": {
		"internal": [
			{
				"role": <personnelrole1>,
				"users":[ <personneluser1>]
			},
			{
				"role": <personnelrole2>,
				"users": [<personneluser2>]
			},
			{
				"role": <personnelrole3>,
				"users":[<personneluser3>]
			},
			{
				"role": <personnelrole4>,
				"users": [<personneluser4>]
			}
		]
	},
		"userRole": <userrole>,
		"user": <user>

}

  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "<errorMessage>".
    Examples:
      |projectName        |projectType|financialYear|projectCompletionDate     |comments         |personnelrole1  |personneluser1                           |personnelrole2    |personneluser2                        |personnelrole3    |personneluser3                       |personnelrole4|personneluser4                        |userrole        |user         |errorMessage            |
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|""                |      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|Bad Request             |
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |""                |       ""                            |"buyer"       |        ""                            |"projectManager"|"KM, Bharath"|Bad Request             |
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |""              |"KM, Bharath"|Missing mandatory fields|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"invalid"       |"KM, Bharath"|Not authorized to access|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |""            |        ""                            |"projectManager"|"KM, Bharath"|Bad Request             |

  @addPALProject_various_userrole
  Scenario Outline: Negative test to create project by passing various userroles
    When the service call for PAL Project  with POST request with "/v1/pal/PALProject/addProject" for negative scenarios with payload with PageObject "FSP PAL Project Details", X-operation "Update", Origin "localhost"
     """
  {
	"information": {
		"projectName":<projectName> ,
		"projectType": <projectType>,
		"financialYear": <financialYear>,
		"projectCompletionDate": <projectCompletionDate>,
		"comments":<comments>
	},
	"personnel": {
		"internal": [
			{
				"role": <personnelrole1>,
				"users":[ <personneluser1>]
			},
			{
				"role": <personnelrole2>,
				"users": [<personneluser2>]
			},
			{
				"role": <personnelrole3>,
				"users":[<personneluser3>]
			},
			{
				"role": <personnelrole4>,
				"users": [<personneluser4>]
			}
		]
	},
		"userRole": <userrole>,
		"user": <user>

}

  """
    Then service should return 400 status code
    Then response must match with json schema "schema/errorResponse.json".
    Then cross verify with the response if the error message is "Not authorized to access".
    Examples:
      |projectName        |projectType|financialYear|projectCompletionDate     |comments         |personnelrole1  |personneluser1                           |personnelrole2    |personneluser2                        |personnelrole3    |personneluser3                       |personnelrole4|personneluser4                        |userrole           |user         |
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"productDeveloper" |"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"foodTechnologist" |"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"buyer"            |"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"supplier"         |"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"ocado"            |"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"international"    |"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"vat"              |"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"design"           |"KM, Bharath"|
      |"Christmas Project"|"Rolling"  |"2021-2022"  |"2022-01-27T00:00:00.000Z"|"Project Created"|"projectManager"|"Prasath.Natarajan@marks-and-spencer.com"|"productDeveloper"|      ""                              |"foodTechnologist"|       ""                            |"buyer"       |        ""                            |"commercialPlanner"|"KM, Bharath"|
