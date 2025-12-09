# Architectural Decision Records

This file records some major [architectural decisions](https://adr.github.io/) that were taken while developing the jPipe compiler. 

We-re using the [_Y-statement_](https://adr.github.io/#sustainable-architectural-decisions) (pronounced "_why statement_") format to document these decisions. See the provided template at the end of this file to record a new one.

## Coding Guidelines

### Documentation

- In the context of _*developing the jPipe compiler*_,
- facing _*a significant drop in code quality and readability*_
- we decided for _*enforcing strict coding guidelines when releasing code*_
  - and neglected _*constant verifications*_,
- to achieve _*an increase in overall code quality when making a PR*_,
  - accepting _*that fixing all these violations is tedious*_,
  - because _*we need to enforce global uniformity at the project level*_.

*Additional information*:
- First versions of jPipe had no coding guidelines enforced. Each
  contributor was coding using their own, making the code base a horrible and unreadable mess.
  We then moved to enforcing constant verification, which ended up slowing down the development
  process as it was not possible to "try" things in the code until it was ready to go into
  production, making "bold" refactoring an issue. The best tradeoff we have found is to allow code
  to not respect these guidelines when developing (e.g., up to `mvn package` phase), but it is
  considered an error to not respect them when releasing (i.e., after `mvn install`, or in the
  CI/CD pipeline).
- We're using customized rules, adapted from Google's checkstyle. We have relaxed two rules: (i)
  code indentation is not restricted to 2 spaces, and (ii) javadoc location is not as strict as
  in the original ruleset, to allow some ASCII art in the code,

### Justification model cloning

- In the context of _implementing composition operators_,
- facing _the need for cloning justification elements_
- we decided for _reimplementing two interfaces (Replicable and ShallowCloneable)_
  - and neglected _using the Cloneable interface provided by Java_
- to achieve _proper implementation of the mechanism_,
  - accepting _re-inventing the wheel (ish)_,
  - because _Java's design does not make a difference between shallow clones and deep clones_.

## Compiler Organization

- In the context of _*compiling a model out of a text file*_,
- facing _*the question of "how to organize the compiler"*_
- we decided for _*modular sources, transformations and sinks*_
    - and neglected _*a monolithic approach*_,
- to achieve _*easy customization of the compilation chain*_,
    - accepting _*a maintenance overhead and potential performance issue (because of calling many transformation)*_,
    - because _*modularity is essential for extensibility of the jPipe compiler*_.


*Additional information*: We apply a functional composition approach where transformations are chained to each other as an ordered sequence, until it produces an output compatible with a provided `Sink`, in charge of serializing it into an output file. This allows users to define their own steps and insert them in the compilation chain, making the compiler extensible.

## Error Management

### Differentiate errors 

- In the context of _*handling exceptions in the source code*_,
- facing _*the need to differentiate Java errors from jPipe errors*_
- we decided for _*modelling jPipe errors as RuntimeException annotated with @JPipeError*_
  - and neglected _*statically checked exceptions*_,
- to achieve _*better error display for the user*_,
  - accepting _*that runtime exceptions might be a pain from a maintenance point of view*_,
  - because _*it provided more flexibility (for now)*_.

### Global error manager

- In the context of _*error management during the compilation process*_,
- facing _*the need to record errors that are not necessarily fatal*_
- we decided for _*a global ErrorManager published as a singleton*_
  - and neglected _*regular exception handling*_,
- to achieve _*centralized error management*_,
  - accepting _*that is triggers weird coding patterns in corner cases*_,
  - because _*centralizing the error processing in the error manager allowed a simpler 
    implementation of error reporting*_.

### Report all errors to the user.

- In the context of _*reporting compilation errors to the user*_, 
- facing _*the question of "when to stop the compilation process"*_ 
- we decided for _*late stop (i.e., collect as many error as possible)*_ 
  - and neglected _*immediate stop when detecting an error*_,
- to achieve _*reporting all the possible errors at once*_, 
  - accepting _*inconsistent models might be passed to next compilation steps*_, 
  - because _*we want to report as many potential errors as possible to the user*_.

*Additional information*: Former versions of the compiler were immediately stopping the compilation process when encountering an error. This ensured models were always correct, but was a pain from an end-user point of view. Running the compiler gives you an error, you fix it, and then it gives you another one, and so on. Collecting everything might end up with _more_ errors (because of cascading effect), but overall ease the usage of the compiler.



## Template

- In the context of _*use case/user story u*_,
- facing _*concern c*_
- we decided for _*option o*_
  - and neglected _*other options*_,
- to achieve _*system qualities/desired consequences*_,
  - accepting _*downside d/undesired consequences*_,
  - because _*additional rationale*_.







