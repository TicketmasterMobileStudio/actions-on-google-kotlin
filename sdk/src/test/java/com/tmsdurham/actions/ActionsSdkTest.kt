package com.tmsdurham.actions

import com.ticketmaster.apiai.DialogState
import com.ticketmaster.apiai.RawInput
import com.tmsdurham.actions.actions.ActionRequest
import com.tmsdurham.actions.actions.ActionResponse
import com.winterbe.expekt.expect
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

typealias MockActionHandler = Handler<ActionRequest, ActionResponse>

//Serializer to handle serialization of conversation token
val serializer: Serializer = object : Serializer {
    override fun <T> serialize(obj: T) = gson.toJson(obj)

    override fun <T> deserialize(str: String, clazz: Class<T>) = gson.fromJson(str, clazz)
}

object ActionsSdkTest : Spek({
    fun requestFromJson(body: String) = gson.fromJson<ActionRequest>(body, ActionRequest::class.java)
    fun responseFromJson(body: String) = gson.fromJson(body, ActionResponse::class.java)

    fun actionsSdkAppRequestBodyNewSession(): ActionRequest {
        return requestFromJson("""{
        "user": {
        "user_id": fakeUserId
    },
        "conversation": {
        "conversation_id": "1480373842830",
        "type": 1
    },
        "inputs": [
        {
            "intent": "assistant.intent.action.MAIN",
            "raw_inputs": [
            {
                "input_type": 2,
                "query": "talk to hello action"
            }
            ],
            "arguments": [
            {
                "name": "agent_info"
            }
            ]
        }
        ]
    }""")
    }

    fun createLiveSessionActionsSdkAppBody(): ActionRequest {
        val tmp = actionsSdkAppRequestBodyNewSession()
        tmp.conversation?.type = "2"
        return tmp
    }
    /**
     * Describes the behavior for ApiAiApp constructor method.
     */
    describe("ActionsSdkApp#constructor") {
        var mockResponse = ResponseWrapper<ActionResponse>()

        beforeEachTest {
            mockResponse = ResponseWrapper<ActionResponse>()
        }

        /*
        // Calls sessionStarted when provided
        it("Calls sessionStarted when new session") {
            val mockRequest = RequestWrapper(headerV1, actionsSdkAppRequestBodyNewSession())

            val app = ActionsSdkApp(
                    request = mockRequest,
                    response = mockResponse,
                    sessionStarted = sessionStartedSpy)
            app.handleRequest({})
            expect(sessionStartedSpy).to.have.been.called()
        }
        */

        /*
        // Does transform to Proto3
        it("Does not detect v2 and transform body when version not present") {
            val mockRequest = RequestWrapper(headerV1, createLiveSessionActionsSdkAppBody())
            val app = ActionsSdkApp(
                    request = mockRequest,
                    response = mockResponse,
                    serializer = serializer
            )
            app.handleRequest({})
            expect(app.request.body).to.equal(requestFromJson("""{
                "user": {
                "userId": fakeUserId
            },
                "conversation": {
                "conversationId": "1480373842830",
                "type": 2
            },
                "inputs": [
                {
                    "intent": "assistant.intent.action.MAIN",
                    "rawInputs": [
                    {
                        "inputType": 2,
                        "query": "talk to hello action"
                    }
                    ],
                    "arguments": [
                    {
                        "name": "agent_info"
                    }
                    ]
                }
                ]
            }"""))
//            expect(app.request.body).to.not.equal(createLiveSessionActionsSdkAppBody())
        }
*/
        // Test a change made for backwards compatibility with legacy sample code
        it("Does initialize StandardIntents without an options object") {
            val app = ActionsSdkApp(RequestWrapper(headerV1, createLiveSessionActionsSdkAppBody()), mockResponse, serializer = serializer)

            expect(app.STANDARD_INTENTS.MAIN).to.equal("assistant.intent.action.MAIN")
            expect(app.STANDARD_INTENTS.TEXT).to.equal("assistant.intent.action.TEXT")
            expect(app.STANDARD_INTENTS.PERMISSION).to
                    .equal("assistant.intent.action.PERMISSION")
        }
    }
    /**
     * Describes the behavior for ActionsSdkApp ask method.
     */
    describe("ActionsSdkApp#ask") {
        var mockRequest = RequestWrapper(headerV1, createLiveSessionActionsSdkAppBody())
        var mockResponse = ResponseWrapper<ActionResponse>()
        var app = ActionsSdkApp(mockRequest, mockResponse, serializer = serializer)

        beforeEachTest {
            mockRequest = RequestWrapper(headerV1, createLiveSessionActionsSdkAppBody())
            mockResponse = ResponseWrapper<ActionResponse>()
            debug("before test: ${mockResponse}")
            app = ActionsSdkApp(
                    request = mockRequest,
                    response = mockResponse, serializer = serializer)
        }

        // Success case test, when the API returns a valid 200 response with the response object
        it("Should return the valid JSON in the response object for the success case.") {
            debug("here 44 ${mockResponse}")
            val inputPrompt = app.buildInputPrompt(true, """<speak>Hi! <break time='1'/> """ +
                    "I can read out an ordinal like " +
                    """<say-as interpret-as='ordinal'>123</say-as>. Say a number.</speak>""",
                    mutableListOf("I didn\'t hear a number", "If you\'re still there, what\'s the number?", "What is the number?"))
            app.ask(inputPrompt)

            // Validating the response object
            val expectedResponse = responseFromJson("""{
                "conversationToken": "{\"state\":null,\"data\":{}}",
                "expectUserResponse": true,
                "expectedInputs": [
                {
                    "inputPrompt": {
                    "initialPrompts": [
                    {
                        "ssml": "<speak>Hi! <break time='1'/> I can read out an ordinal like <say-as interpret-as='ordinal'>123</say-as>. Say a number.</speak>"
                    }
                    ],
                    "noInputPrompts": [
                    {
                        "ssml": "I didn\'t hear a number"
                    },
                    {
                        "ssml": "If you\'re still there, what\'s the number?"
                    },
                    {
                        "ssml": "What is the number?"
                    }
                    ]
                },
                    "possibleIntents": [
                    {
                        "intent": "assistant.intent.action.TEXT"
                    }
                    ]
                }
                ]
            }""")
            expect(mockResponse.body).to.equal(expectedResponse)
        }

        it("Should return the valid JSON in the response object for the success case when String text was asked w/o input prompts.") {
            app.ask("What can I help you with?")
            val expectedResponse = responseFromJson("""{
                "conversationToken": "{\"state\":null,\"data\":{}}",
                "expectUserResponse": true,
                "expectedInputs": [
                {
                    "inputPrompt": {
                    "initialPrompts": [
                    {
                        "textToSpeech": "What can I help you with?"
                    }
                    ],
                    "noInputPrompts": [

                    ]
                },
                    "possibleIntents": [
                    {
                        "intent": "assistant.intent.action.TEXT"
                    }
                    ]
                }
                ]
            }""")
            expect(mockResponse.body).to.equal(expectedResponse)
        }

        it("Should return the valid JSON in the response object for the success case when SSML text was asked w/o input prompts.") {
            app.ask("<speak>What <break time=\"1\"/> can I help you with?</speak>")
            // Validating the response object
            val expectedResponse = responseFromJson("""{
                "conversationToken": "{\"state\":null,\"data\":{}}",
                "expectUserResponse": true,
                "expectedInputs": [
                {
                    "inputPrompt": {
                    "initialPrompts": [
                    {
                        "ssml": "<speak>What <break time=\"1\"/> can I help you with?</speak>"
                    }
                    ],
                    "noInputPrompts": [

                    ]
                },
                    "possibleIntents": [
                    {
                        "intent": "assistant.intent.action.TEXT"
                    }
                    ]
                }
                ]
            }""")
            expect(mockResponse.body).to.equal(expectedResponse)
        }

        it("Should return the valid JSON in the response object for the advanced success case.") {
            val inputPrompt = app.buildInputPrompt(false, "Welcome to action snippets! Say a number.",
                    mutableListOf("Say any number", "Pick a number", "What is the number?"))
            app.ask(inputPrompt)
            // Validating the response object
            val expectedResponse = responseFromJson("""{
            "conversationToken": "{\"state\":null,\"data\":{}}",
            "expectUserResponse": true,
            "expectedInputs": [
            {
                "inputPrompt": {
                "initialPrompts": [
                {
                    "textToSpeech": "Welcome to action snippets! Say a number."
                }
                ],
                "noInputPrompts": [
                {
                    "textToSpeech": "Say any number"
                },
                {
                    "textToSpeech": "Pick a number"
                },
                {
                    "textToSpeech": "What is the number?"
                }
                ]
            },
                "possibleIntents": [
                {
                    "intent": "assistant.intent.action.TEXT"
                }
                ]
            }
            ]
        }""")
            expect(mockResponse.body).to.equal(expectedResponse)
        }

        it("Should return the valid simple response JSON in the response object for the success case.") {
            app.ask {
                textToSpeech = "hello"
                displayText = "hi"
            }
            // Validating the response object
            val expectedResponse = responseFromJson("""{
                "conversationToken": "{\"state\":null,\"data\":{}}",
                "expectUserResponse": true,
                "expectedInputs": [
                {
                    "inputPrompt": {
                    "richInitialPrompt": {
                    "items": [
                    {
                        "simpleResponse": {
                        "textToSpeech": "hello",
                        "displayText": "hi"
                    }
                    }
                    ],
                    "suggestions": []
                }
                },
                    "possibleIntents": [
                    {
                        "intent": "assistant.intent.action.TEXT"
                    }
                    ]
                }
                ]
            }""")
            expect(mockResponse.body).to.equal(expectedResponse)
        }

        // Success case test, when the API returns a valid 200 response with the response object
        it("Should return the valid rich response JSON in the response object for the success case.") {
            app.ask(app.buildRichResponse()
                    .addSimpleResponse(speech = "hello", displayText = "hi")
                    .addSuggestions("Say this", "or this"))

            // Validating the response object
            val expectedResponse = responseFromJson("""{
                "conversationToken": "{\"state\":null,\"data\":{}}",
                "expectUserResponse": true,
                "expectedInputs": [
                {
                    "inputPrompt": {
                    "richInitialPrompt": {
                    "items": [
                    {
                        "simpleResponse": {
                        "textToSpeech": "hello",
                        "displayText": "hi"
                    }
                    }
                    ],
                    "suggestions": [
                    {
                        "title": "Say this"
                    },
                    {
                        "title": "or this"
                    }
                    ]
                }
                },
                    "possibleIntents": [
                    {
                        "intent": "assistant.intent.action.TEXT"
                    }
                    ]
                }
                ]
            }""")
            expect(mockResponse.body).to.equal(expectedResponse)
        }

    }

    /**
     * Describes the behavior for ActionsSdkApp tell method.
     */
    describe("ActionsSdkApp#tell") {
        var mockRequest = RequestWrapper(headerV1, createLiveSessionActionsSdkAppBody())
        var mockResponse = ResponseWrapper<ActionResponse>()
        var app = ActionsSdkApp(mockRequest, mockResponse, serializer = serializer)

        beforeEachTest {
            mockRequest = RequestWrapper(headerV1, createLiveSessionActionsSdkAppBody())
            mockResponse = ResponseWrapper<ActionResponse>()
            debug("before test: ${mockResponse}")
            app = ActionsSdkApp(
                    request = mockRequest,
                    response = mockResponse, serializer = serializer)
        }

        // Success case test, when the API returns a valid 200 response with the response object
        it("Should return the valid JSON in the response object for the success case.") {
            app.tell("Goodbye!")
            val expectedResponse = responseFromJson("""{
                "expectUserResponse": false,
                "finalResponse": {
                "speechResponse": {
                "textToSpeech": "Goodbye!"
            }
            }
            }""")
            expect(mockResponse.body).to.equal(expectedResponse)
        }

        // Success case test, when the API returns a valid 200 response with the response object
        it("Should return the valid simple rich response JSON in the response object for the success case.") {
            app.tell(speech = "hello", displayText = "hi")

            // Validating the response object
            val expectedResponse = responseFromJson("""{
                "expectUserResponse": false,
                "finalResponse": {
                "richResponse": {
                "items": [
                {
                    "simpleResponse": {
                    "textToSpeech": "hello",
                    "displayText": "hi"
                }
                }
                ],
                "suggestions": []
            }
            }
            }""")
            expect(mockResponse.body)
                    .to.equal(expectedResponse)
        }

        // Success case test, when the API returns a valid 200 response with the response object
        it("Should return the valid rich response JSON in the response object for the success case.") {
            app.tell(app.buildRichResponse()
                    .addSimpleResponse(speech = "hello", displayText = "hi")
                    .addSuggestions("Say this", "or this"))

            // Validating the response object
            val expectedResponse = responseFromJson("""{
                "expectUserResponse": false,
                "finalResponse": {
                "richResponse": {
                "items": [
                {
                    "simpleResponse": {
                    "textToSpeech": "hello",
                    "displayText": "hi"
                }
                }
                ],
                "suggestions": [
                {
                    "title": "Say this"
                },
                {
                    "title": "or this"
                }
                ]
            }
            }
            }""")
            expect(mockResponse.body)
                    .to.equal(expectedResponse)
        }

        // Failure test, when the API returns a 400 response with the response object
        it("Should send failure response for rich response without simple response") {
            fun handler(app: ActionsSdkApp) = app.tell(app.buildRichResponse())

            val actionMap = mapOf("intent_name_not_present_in_the_body" to ::handler)

            app.handleRequest(actionMap)

            expect(mockResponse.statusCode).to.equal(400)
        }
    }

    /**
     * Describes the behavior for ActionsSdkApp getRawInput method.
     */
    describe("ActionsSdkApp#getRawInput") {
        // Success case test, when the API returns a valid 200 response with the response object
        it("Should get the raw user input for the success case.") {
            val body = createLiveSessionActionsSdkAppBody()
            body.inputs!![0].rawInputs = mutableListOf(gson.fromJson("""
            {
                "inputType": 2,
                "query": "bye"
            }
            """, RawInput::class.java))
            val mockRequest = RequestWrapper(headerV1, body)
            val mockResponse = ResponseWrapper<ActionResponse>()
            val app = ActionsSdkApp(
                    request = mockRequest,
                    response = mockResponse,
                    serializer = serializer)
            expect(app.getRawInput()).to.equal("bye")
        }
    }

    /**
     * Describes the behavior for ActionsSdkApp askWithList method.
     */
    describe("ActionsSdkApp#askWithList") {
        var mockRequest = RequestWrapper(headerV1, createLiveSessionActionsSdkAppBody())
        var mockResponse = ResponseWrapper<ActionResponse>()
        var app = ActionsSdkApp(mockRequest, mockResponse, serializer = serializer)

        beforeEachTest {
            mockRequest = RequestWrapper(headerV1, createLiveSessionActionsSdkAppBody())
            mockResponse = ResponseWrapper<ActionResponse>()
            debug("before test: ${mockResponse}")
            app = ActionsSdkApp(
                    request = mockRequest,
                    response = mockResponse, serializer = serializer)
        }

        // Success case test, when the API returns a valid 200 response with the response object
        it("Should return the valid list JSON in the response object for the success case.") {
            app.askWithList("Here is a list", app.buildList()
                    .addItems(
                    app.buildOptionItem("key_1", "key one"),
                    app.buildOptionItem("key_2", "key two")
                    ), DialogState(
                optionType = "list"))

            // Validating the response object
            val expectedResponse = responseFromJson("""{
                "conversationToken": "{"optionType":"list"}",
                "expectUserResponse": true,
                "expectedInputs": [
                {
                    "inputPrompt": {
                    "initialPrompts": [
                    {
                        "textToSpeech": "Here is a list"
                    }
                    ],
                    "noInputPrompts": [
                    ]
                },
                    "possibleIntents": [
                    {
                        "intent": "actions.intent.OPTION",
                        "inputValueData": {
                        "@type": "type.googleapis.com/google.actions.v2.OptionValueSpec",
                        "listSelect": {
                        "items": [
                        {
                            "optionInfo": {
                            "key": "key_1",
                            "synonyms": [
                            "key one"
                            ]
                        },
                            "title": ""
                        },
                        {
                            "optionInfo": {
                            "key": "key_2",
                            "synonyms": [
                            "key two"
                            ]
                        },
                            "title": ""
                        }
                        ]
                    }
                    }
                    }
                    ]
                }
                ]
            }""")

            expect(mockResponse.body).to.equal(expectedResponse)
        }

        it("Should return the an error JSON in the response when list has <2 items.") {
            app.askWithList("Here is a list", app.buildList(), DialogState(
                optionType = "list"))
            expect(mockResponse.statusCode).to.equal(400)
        }
    }

})