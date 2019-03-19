import WorkflowUI

final class SamplePickerViewController: ScreenViewController<SamplePickerScreen>, UITableViewDataSource, UITableViewDelegate {

    private let tableView = UITableView(frame: .zero, style: .plain)

    override func loadView() {
        tableView.dataSource = self
        tableView.delegate = self
        self.view = tableView
    }

    override func screenDidChange(from previousScreen: SamplePickerScreen) {
        super.screenDidChange(from: previousScreen)
        tableView.reloadData()
    }

    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return screen.samples.count
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = UITableViewCell(style: .subtitle, reuseIdentifier: nil)
        let sample = screen.samples[indexPath.row]
        cell.textLabel?.text = sample.title
        cell.detailTextLabel?.text = sample.subtitle
        cell.accessoryType = .disclosureIndicator
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let sample = screen.samples[indexPath.row]
        screen.onSelectSample.send(sample)
    }
}
