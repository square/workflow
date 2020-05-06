/*
 * Copyright 2020 Square Inc.
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

import ReactiveSwift

final class AuthenticationService {
    static let delayMS: TimeInterval = 0.750
    static let weakToken = "Need a second factor there, friend"
    static let realToken = "Welcome aboard!"
    static let secondFactor = "1234"

    func login(email: String, password: String) -> SignalProducer<AuthenticationResponse, AuthenticationError> {
        if password == "password" {
            if email.contains("2fa") {
                return SignalProducer(value: AuthenticationResponse(
                    token: AuthenticationService.weakToken,
                    secondFactorRequired: true
                ))
                    .delay(AuthenticationService.delayMS, on: QueueScheduler.main)
            } else {
                return SignalProducer(value: AuthenticationResponse(
                    token: AuthenticationService.realToken, secondFactorRequired: false
                ))
                    .delay(AuthenticationService.delayMS, on: QueueScheduler.main)
            }
        } else {
            return SignalProducer(error: .invalidUserPassword)
                .delay(AuthenticationService.delayMS, on: QueueScheduler.main)
        }
    }

    func secondFactor(token: String, secondFactor: String) -> SignalProducer<AuthenticationResponse, AuthenticationError> {
        if token != AuthenticationService.weakToken {
            return SignalProducer(error: .invalidIntermediateToken)
                .delay(AuthenticationService.delayMS, on: QueueScheduler.main)
        } else if secondFactor != AuthenticationService.secondFactor {
            return SignalProducer(error: .invalidTwoFactor)
                .delay(AuthenticationService.delayMS, on: QueueScheduler.main)
        } else {
            return SignalProducer(value: AuthenticationResponse(
                token: AuthenticationService.realToken,
                secondFactorRequired: false
            ))
                .delay(AuthenticationService.delayMS, on: QueueScheduler.main)
        }
    }
}

extension AuthenticationService {
    enum AuthenticationError: Error {
        var localizedDescription: String {
            switch self {
            case .invalidUserPassword:
                return "Unknown user or invalid password."
            case .invalidTwoFactor:
                return "Invalid second factor (try \(AuthenticationService.secondFactor))"
            case .invalidIntermediateToken:
                return "404!! What happened to your token there bud?!?!"
            }
        }

        case invalidUserPassword
        case invalidTwoFactor
        case invalidIntermediateToken
    }

    struct AuthenticationResponse {
        var token: String
        var secondFactorRequired: Bool
    }
}
