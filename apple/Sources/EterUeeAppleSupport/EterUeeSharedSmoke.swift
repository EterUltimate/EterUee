import Foundation
import EterUeeShared

public enum EterUeeSharedSmoke {
    public static func frameworkName() -> String {
        EterUeeAppleBridge.shared.frameworkName()
    }

    public static func runtimeCapabilitiesJSON() -> String {
        EterUeeAppleBridge.shared.runtimeCapabilitiesJson()
    }

    public static func sampleRoleplayPrompt() -> String {
        EterUeeAppleBridge.shared.sampleRoleplayPrompt()
    }

    public static func buildRoleplayPromptJSON(_ requestJSON: String) -> String {
        EterUeeAppleBridge.shared.buildRoleplayPromptJson(requestJson: requestJSON)
    }
}
