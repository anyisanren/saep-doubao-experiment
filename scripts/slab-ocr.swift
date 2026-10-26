import Foundation
import AppKit
import Vision

let url = URL(fileURLWithPath: CommandLine.arguments[1])
let source = CGImageSourceCreateWithURL(url as CFURL, nil)!
let image = CGImageSourceCreateImageAtIndex(source, 0, nil)!
let request = VNRecognizeTextRequest()
request.recognitionLevel = .accurate
request.recognitionLanguages = ["zh-Hans", "en-US"]
request.usesLanguageCorrection = false
try VNImageRequestHandler(cgImage: image).perform([request])
let rows = (request.results ?? []).compactMap { item -> [String: Any]? in
    guard let text = item.topCandidates(1).first else { return nil }
    let box = item.boundingBox
    return ["text": text.string, "confidence": text.confidence,
            "x": box.midX * Double(image.width),
            "y": (1 - box.midY) * Double(image.height),
            "left": box.minX * Double(image.width),
            "right": box.maxX * Double(image.width),
            "top": (1 - box.maxY) * Double(image.height),
            "bottom": (1 - box.minY) * Double(image.height)]
}
let data = try JSONSerialization.data(withJSONObject: rows, options: [.prettyPrinted, .sortedKeys])
print(String(data: data, encoding: .utf8)!)
