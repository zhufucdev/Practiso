import Foundation
import SwiftUI
import ComposeApp

struct TextFrameView : View {
    var frame: TextFrame
    var body: some View {
        Text(frame.content)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}
