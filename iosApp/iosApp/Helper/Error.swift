import Foundation
import ComposeApp

extension String {
    init(appScope: AppScope) {
        switch appScope {
        case .libraryIntentModel:
            self.init(localized: "library intent model")
        case .feiInitialization:
            self.init(localized: "frame embedding inference initialization")
        case .feiResource:
            self.init(localized: "frame embedding inference resource")
        case .unknown:
            self.init(localized: "unknown scope")
        }
    }
    
    init(errorMessage: ErrorMessage) {
        switch onEnum(of: errorMessage) {
        case .copyResource(_):
            self.init(localized: "Failed to copy resource.")
        case .incompatibleModel(_):
            self.init(localized: "Incompatible model.")
        case .invalidFileFormat(_):
            self.init(localized: "Invalid file format.")
        case .resourceNotFound(let res):
            self.init(localized: "Resource \(res.name) at \(res.location) was not found.")
        case .raw(let raw):
            self.init(raw.content)
        case .localized(let localized):
            self.init(localized: localized.resource as! String.LocalizationValue)
        }
    }
}
