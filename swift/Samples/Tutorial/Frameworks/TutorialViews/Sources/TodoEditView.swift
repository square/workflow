/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import UIKit

public final class TodoEditView: UIView, UITextViewDelegate {
    public var title: String {
        didSet {
            titleField.text = title
        }
    }

    public var note: String {
        didSet {
            noteField.text = note
        }
    }

    public var onTitleChanged: (String) -> Void
    public var onNoteChanged: (String) -> Void

    let titleField: UITextField
    let noteField: UITextView

    override public init(frame: CGRect) {
        self.title = ""
        self.note = ""
        self.onTitleChanged = { _ in }
        self.onNoteChanged = { _ in }

        self.titleField = UITextField(frame: .zero)
        self.noteField = UITextView(frame: .zero)

        super.init(frame: frame)

        backgroundColor = .white

        titleField.textAlignment = .center
        titleField.addTarget(self, action: #selector(titleDidChange(sender:)), for: .editingChanged)
        titleField.layer.borderColor = UIColor.black.cgColor
        titleField.layer.borderWidth = 1.0

        noteField.delegate = self
        noteField.layer.borderColor = UIColor.gray.cgColor
        noteField.layer.borderWidth = 1.0

        addSubview(titleField)
        addSubview(noteField)
    }

    @available(*, unavailable)
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override public func layoutSubviews() {
        super.layoutSubviews()

        let titleHeight: CGFloat = 44.0
        let spacing: CGFloat = 8.0
        let widthInset: CGFloat = 8.0

        var yOffset = bounds.minY

        titleField.frame = CGRect(
            x: bounds.minX,
            y: yOffset,
            width: bounds.maxX,
            height: titleHeight
        )
        .insetBy(dx: widthInset, dy: 0.0)

        yOffset += titleHeight + spacing

        noteField.frame = CGRect(
            x: bounds.minX,
            y: yOffset,
            width: bounds.maxX,
            height: bounds.maxY - yOffset
        )
        .insetBy(dx: widthInset, dy: 0.0)
    }

    @objc private func titleDidChange(sender: UITextField) {
        guard let titleText = sender.text else {
            return
        }

        onTitleChanged(titleText)
    }

    // MARK: UITextFieldDelegate

    @objc public func textViewDidChange(_ textView: UITextView) {
        guard textView === noteField else {
            return
        }

        guard let noteText = textView.text else {
            return
        }

        onNoteChanged(noteText)
    }
}
