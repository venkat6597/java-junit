#Author: MadhuShreeVarshini.Chandrasekar@mnscorp.net
#Feature:Get List Templates Functionality
#StoryDiscription  : This endpoint is to fetch the list of templates in PAL
@GetListTemplates
Feature: Get List Templates Functionality
  Scenario: Test to get list of listTemplate for Pal Service
    Given Get service call for users list templates endpoint "/v1/pal/listTemplates"
    Then service should return 200 status code
    Then response must match with json schema "schema/listTemplates.json".
    Then cross verify with the response if the id is "611d11f7cde71e991b217579" and name is "Standard"
