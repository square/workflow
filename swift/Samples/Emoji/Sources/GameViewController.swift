import UIKit
import Workflow
import WorkflowUI


final class EmojiViewController: ScreenViewController<EmojiScreen> {

    private let instructionsLabel = UILabel()

    private let mapView = MapView()

    private let moneyLabel = UILabel()

    private let resetButton = UIButton(type: .system)
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.white

        resetButton.setTitle("Reset", for: .normal)
        resetButton.addTarget(self, action: #selector(reset), for: .touchUpInside)
        view.addSubview(resetButton)

        instructionsLabel.text = "Tap to move. Avoid monsters. Get money."
        instructionsLabel.font = UIFont.systemFont(ofSize: 11)
        instructionsLabel.textColor = .darkGray
        view.addSubview(instructionsLabel)

        view.addSubview(mapView)

        view.addSubview(moneyLabel)

        let tapRecognizer = UITapGestureRecognizer(target: self, action: #selector(tapped))
        view.addGestureRecognizer(tapRecognizer)

        updateView()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        let mapSize = view.bounds.width - 40.0
        mapView.bounds = CGRect(x: 0, y: 0, width: mapSize, height: mapSize)
        mapView.center = CGPoint(x: view.bounds.midX, y: view.bounds.midY)

        instructionsLabel.sizeToFit()
        instructionsLabel.center.x = view.bounds.midX
        instructionsLabel.center.y = mapView.frame.minY - 8.0 - instructionsLabel.bounds.height/2.0


        moneyLabel.sizeToFit()
        moneyLabel.center = CGPoint(x: view.bounds.midX, y: mapView.frame.maxY + 4.0 + moneyLabel.bounds.height/2.0)

        resetButton.bounds = CGRect(x: 0.0, y: 0.0, width: view.bounds.width, height: 44.0)
        resetButton.center = CGPoint(
            x: view.bounds.midX,
            y: mapView.frame.maxY + ((view.bounds.maxY - mapView.frame.maxY) * 0.5))
    }

    override func screenDidChange(from previousScreen: EmojiScreen) {
        super.screenDidChange(from: previousScreen)
        updateView()
    }

    private func updateView() {
        mapView.state = screen.gameState
        moneyLabel.text = Array(
            repeating: "💰",
            count: screen.gameState.playerMoney)
            .reduce("", +)
        view.setNeedsLayout()
    }

    @objc private func reset() {
        screen.sink.send(.reset)
    }

    @objc private func tapped(tapRecogizer: UITapGestureRecognizer) {

        // Get delta from center of the view
        var tapDelta = tapRecogizer.location(in: view)
        tapDelta.x -= view.bounds.width/2.0
        tapDelta.y -= view.bounds.height/2.0

        if abs(tapDelta.x) > abs(tapDelta.y) {
            // Horizontal
            if tapDelta.x > 0 {
                screen.sink.send(.move(.right))
            } else {
                screen.sink.send(.move(.left))
            }
        } else {
            // Vertical
            if tapDelta.y > 0 {
                screen.sink.send(.move(.down))
            } else {
                screen.sink.send(.move(.up))
            }
        }

    }
}


fileprivate final class MapView: UIView {

    private let font: UIFont = .systemFont(ofSize: 18.0)

    var state: GameState? = nil {
        didSet {
            setNeedsDisplay()
        }
    }


    override func draw(_ rect: CGRect) {
        UIColor.brown.setFill()
        UIBezierPath(rect: bounds).fill()

        guard let state = state else { return }

        let tileWidth = bounds.width / CGFloat(Map.width)
        let tileHeight = bounds.height / CGFloat(Map.height)

        for position in state.map.allPositions {
            let text = state.text(at: position) as NSString
            let size = (text)
                .boundingRect(
                    with: .zero,
                    options: [],
                    attributes: [.font: font],
                    context: nil)
                .size

            let positionX = (CGFloat(position.x) * tileWidth) + (tileWidth * 0.5) - (size.width * 0.5)
            let positionY = (CGFloat(position.y) * tileHeight) + (tileHeight * 0.5) - (size.height * 0.5)

            text.draw(
                at: CGPoint(
                    x: positionX ,
                    y: positionY),
                withAttributes: [.font: font])
        }

    }


}
