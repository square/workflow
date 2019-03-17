import UIKit

extension Sample {

    // Add additional samples here to show them in the sample app navigation
    static var all: [Sample] {
        return [
            Sample(
                title: "Emoji Game",
                subtitle: "Single view demonstrating a basic input loop",
                viewControllerType: GameViewController.self)
        ]
    }

}

fileprivate struct Sample {
    var title: String
    var subtitle: String
    var viewControllerBuilder: () -> UIViewController

    init(title: String, subtitle: String, viewControllerBuilder: @escaping () -> UIViewController) {
        self.title = title
        self.subtitle = subtitle
        self.viewControllerBuilder = viewControllerBuilder
    }

    init<T: UIViewController>(title: String, subtitle: String, viewControllerType: T.Type) {
        self.init(
            title: title,
            subtitle: subtitle,
            viewControllerBuilder: { T() })
    }

}


final class ViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {

    private let samples = Sample.all

    private let tableView = UITableView(frame: .zero, style: .plain)

    init() {
        super.init(nibName: nil, bundle: nil)
        navigationItem.title = "Workflow Samples"
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func loadView() {
        tableView.dataSource = self
        tableView.delegate = self
        self.view = tableView
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        for indexPath in tableView.indexPathsForSelectedRows ?? [] {
            tableView.deselectRow(at: indexPath, animated: true)
        }
    }

    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return samples.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = UITableViewCell(style: .subtitle, reuseIdentifier: nil)
        cell.textLabel?.text = samples[indexPath.row].title
        cell.detailTextLabel?.text = samples[indexPath.row].subtitle
        cell.accessoryType = .disclosureIndicator
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let vc = samples[indexPath.row].viewControllerBuilder()
        navigationController?.pushViewController(vc, animated: true)
    }

}

