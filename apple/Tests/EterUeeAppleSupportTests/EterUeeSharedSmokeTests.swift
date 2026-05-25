import Testing
@testable import EterUeeAppleSupport

@Test func kotlinFrameworkBridgeIsImportableFromSwift() {
    #expect(EterUeeSharedSmoke.frameworkName() == "EterUeeShared")
    #expect(EterUeeSharedSmoke.runtimeCapabilitiesJSON().contains("appleTargets"))
}

@Test func roleplayPromptBridgeBuildsPromptFromJSON() {
    let requestJSON = """
    {
      "systemPrompt": "Stay in character.",
      "worldInfoEntries": [
        {
          "key": "Arcadia",
          "content": "Arcadia is a port city.",
          "position": "BEFORE_LAST_USER_MESSAGE"
        }
      ],
      "messages": [
        {
          "role": "USER",
          "content": "Where is Arcadia?"
        }
      ],
      "matchWorldInfoAgainst": "Arcadia"
    }
    """

    let resultJSON = EterUeeSharedSmoke.buildRoleplayPromptJSON(requestJSON)

    #expect(resultJSON.contains("Arcadia is a port city."))
    #expect(resultJSON.contains(#""injectedEntryCount":1"#))
}
