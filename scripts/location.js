import csv from 'csvtojson'
import axios from 'axios'
import { appendFileSync } from 'fs'
import oauth from 'axios-oauth-client'

// Set the CSV file path and endpoint URL
const csvFilePath = 'csv/khmfl_test.csv'
const endpointUrl = 'https:/example.com/fhir-server/api/v4/Location'
const outputFilePath = './output/khmfl_shr.txt'

// Set the authentication information for basic auth
const username = ''
const password = ''

// Set the authentication information for OAuth2
const clientId = 'partner.test.client'
const clientSecret = 'partnerTestPwd'
const tokenEndpoint =
  ''

// Set the authentication type (basic or oauth) based on user input
const authType = 'basic' // or "oauth"

// Read CSV file
const jsonArray = await
csv().fromFile(csvFilePath)

for (let i = 0; i < jsonArray.length; i++) {
  const row = jsonArray[i]

  if (row.Code !== 'None') {
    console.log(row.Code)
    try {
      const now = new Date()
      now.toLocaleString()
      const location = {
        resourceType: 'Location',
        text: {
          status: 'generated',
          div:
            '<div xmlns="http://www.w3.org/1999/xhtml"><h2>' +
            row.Name +
            '</h2><h2><span>' +
            row.County +
            ' </span><span>Kenya </span></h2></div>',
        },
        identifier: [
          {
            system: 'https://nhdd.health.go.ke/#/orgs/MOH-KENYA/sources/KMHFL',
            value: row.Code,
          },
        ],
        status: 'active',
        name: row.Name,
        description: row.Facilitytype,
        address: {
          extension: [
            {
              url: 'http://fhir.openmrs.org/ext/address',
              extension: [
                {
                  url: 'http://fhir.openmrs.org/ext/address#address5',
                  valueString: row.County,
                },
                {
                  url: 'http://fhir.openmrs.org/ext/address#address6',
                  valueString: row.Subcounty,
                },
              ],
            },
          ],
          state: row.County,
          country: 'Kenya',
        },
      }

      let config = {
        method: 'post',
        maxBodyLength: Infinity,
        url: endpointUrl,
        data: location,
      }

      // Set authentication headers based on the chosen authType
      if (authType === 'basic') {
        config.headers = {
          'Content-Type': 'application/json',
          Authorization:
            'Basic ' +
            Buffer.from(username + ':' + password).toString('base64'),
        }
      } else if (authType === 'oauth') {
        const oauthClient = new oauth.clientCredentials(
          axios.create({
            method: 'post',
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded',
            },
          }),
          tokenEndpoint,
          clientId,
          clientSecret,
        )

        const token = await
        oauthClient()

        config.headers = {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token.access_token}`,
        }
      }

      // Send POST request to endpoint with CSV data and authentication headers
      const response = await
      axios(config)

      const url = new URL(response.headers.location)
      const path = url.pathname.split('/')
      const uuid = path[path.length - 3]
      console.log(url, uuid, response)
      appendFileSync(outputFilePath, row.Code + ',' + uuid + '\n')
    } catch (error) {
      console.log(error.response.data.issue[0].details)
      console.error(
        `Error sending POST request for row ${i}: ${error.response}`)
    }
  }
}
