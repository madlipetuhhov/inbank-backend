# Task

Validate TICKET-101 and write a conclusion for it as .md in the project directory. Make sure
that the code works according to the requirements. Make sure to highlight what the intern
did well as places for improvement. Tip: The more SOLID the better.
Highlight the most important shortcoming of TICKET-101 with explanation. Fix it!

# Review

NB! Feedback about front-end is in the .md file in the front-end in a file "TICKET-101.md".

Decision engine takes in personal code, loan amount, loan period in months - implemented correctly.

The idea of the decision engine is to determine what would be the maximum sum, regardless of the
person requested loan amount. This is partly implemented:
  - Decision engine does not return a larger amount, if the allowed amount is more than the requested amount.
  - Decision engine returns a smaller amount, if the allowed amount is less than the requested amount.

If a suitable loan amount is not found within the selected period, the decision engine should also try to find a new
suitable period - this is not implemented. Decision engine does not offer a longer loan period for the requested amount
and returns negative response message.

It supports 4 different scenarios. The solution is simple and well done! Though would have expected more guidance from the task.

Constraints are implemented correctly in back-end, one change needs to be made in the front-end (6 months to 12 months).

Scoring algorithm is not implemented, therefore credit score is not calculated and is not taken into account when
returning the final result. This is the most important shortcoming and should be fixed.

The logic of the folders needs some correction - to separate the business logic from the infrastructure.

Exceptions are thoughtfully done. It can be seen that various scenarios have been analysed.

Although the application does not seem to be able to set a larger or smaller amount / period than the leverage limit,
the entered loan amounts and periods are limited in the verifyInputs method. Good job!

# Fixes

In the method verifyInputs reversing the conditions for checking loan amount and loan period
against their limitations will make the code more readable.

Implemented scoring algorithm in the method getHighestValidLoanAmount.
When fixing, I assumed that there was one mistake in the wording of the task. In the following sentence modifier was
meant instead of identifier: "If a person has no debt then we take the identifier and use it for calculating a person's
credit score taking into account the requested input."

Fixed the method getHighestValidLoanAmount. It will return a larger amount, if the allowed amount is more than the requested amount.

Changed the logic of folders in the project - separated business logic from infrastructure.