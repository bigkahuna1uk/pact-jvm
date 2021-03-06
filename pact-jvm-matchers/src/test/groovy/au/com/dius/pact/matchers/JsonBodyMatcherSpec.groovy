package au.com.dius.pact.matchers

import au.com.dius.pact.model.BodyMismatch
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.matchingrules.MatchingRules
import au.com.dius.pact.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.model.matchingrules.RegexMatcher
import au.com.dius.pact.model.matchingrules.TypeMatcher
import spock.lang.Specification

import static scala.collection.JavaConverters.asJavaCollection

class JsonBodyMatcherSpec extends Specification {

  private matchers
  private final JsonBodyMatcher matcher = new JsonBodyMatcher()
  private expected, actual

  def setup() {
    matchers = new MatchingRules()
    expected = { body -> new Request('', '', null, null, body, matchers) }
    actual = { body -> new Request('', '', null, null, body) }
  }

  def 'matching json bodies - return no mismatches - when comparing empty bodies'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.empty()
  }

  def 'matching json bodies - return no mismatches - when comparing a missing body to anything'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('"Blah"')
    expectedBody = OptionalBody.missing()
  }

  def 'matching json bodies - return no mismatches - with equal bodies'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('"Blah"')
    expectedBody = OptionalBody.body('"Blah"')
  }

  def 'matching json bodies - return no mismatches - with equal Maps'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('{"something": 100}')
    expectedBody = OptionalBody.body('{"something":100}')
  }

  def 'matching json bodies - return no mismatches - with equal Lists'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('[100,200,300]')
    expectedBody = OptionalBody.body('[100, 200, 300]')
  }

  def 'matching json bodies - return no mismatches - with each like matcher on unequal lists'() {
    given:
    matchers.addCategory('body').addRule('$.list', new MinTypeMatcher(1))

    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('{"list": [100, 200, 300, 400]}')
    expectedBody = OptionalBody.body('{"list": [100]}')
  }

  def 'matching json bodies - return no mismatches - with each like matcher on empty list'() {
    given:
    matchers.addCategory('body').addRule('$.list', new MinTypeMatcher(0))

    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('{"list": []}')
    expectedBody = OptionalBody.body('{"list": [100]}')
  }

  def 'matching json bodies - returns a mismatch - when comparing anything to an empty body'() {
    expect:
    !matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('')
    expectedBody = OptionalBody.body('"Blah"')
  }

  def 'matching json bodies - returns a mismatch - when comparing anything to a null body'() {
    expect:
    !matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('""')
    expectedBody = OptionalBody.nullBody()
  }

  def 'matching json bodies - returns a mismatch - when comparing an empty map to a non-empty one'() {
    expect:
    asJavaCollection(matcher.matchBody(expected(expectedBody), actual(actualBody), true)).find {
      it instanceof BodyMismatch &&
        it.mismatch.get().contains('Expected an empty Map but received Map(something -> 100)')
    }

    where:

    actualBody = OptionalBody.body('{"something": 100}')
    expectedBody = OptionalBody.body('{}')
  }

  def 'matching json bodies - returns a mismatch - when comparing an empty list to a non-empty one'() {
    expect:
    asJavaCollection(matcher.matchBody(expected(expectedBody), actual(actualBody), true)).find {
      it instanceof BodyMismatch &&
        it.mismatch.get().contains('Expected an empty List but received List(100)')
    }

    where:

    actualBody = OptionalBody.body('[100]')
    expectedBody = OptionalBody.body('[]')
  }

  def 'matching json bodies - returns a mismatch - when comparing a map to one with less entries'() {
    expect:
    asJavaCollection(matcher.matchBody(expected(expectedBody), actual(actualBody), true)).find {
      it instanceof BodyMismatch &&
        it.mismatch.get().contains('Expected a Map with at least 2 elements but received 1 elements')
    }

    where:

    actualBody = OptionalBody.body('{"something": 100}')
    expectedBody = OptionalBody.body('{"something": 100, "somethingElse": 100}')
  }

  def 'matching json bodies - returns a mismatch - when comparing a list to one with with different size'() {
    given:
    def actualBody = OptionalBody.body('[1,2,3]')
    def expectedBody = OptionalBody.body('[1,2,3,4]')

    when:
    def mismatches = asJavaCollection(matcher.matchBody(expected(expectedBody), actual(actualBody), true)).findAll {
      it instanceof BodyMismatch
    }.collect {
      it.mismatch.get()
    }

    then:
    mismatches.size() == 2
    mismatches.contains('Expected a List with 4 elements but received 3 elements')
    mismatches.contains('Expected 4 but was missing')
  }

  def 'matching json bodies - returns a mismatch - when the actual body is missing a key'() {
    expect:
    asJavaCollection(matcher.matchBody(expected(expectedBody), actual(actualBody), true)).find {
      it instanceof BodyMismatch &&
        it.mismatch.get().contains('Expected somethingElse=100 but was missing')
    }

    where:

    actualBody = OptionalBody.body('{"something": 100}')
    expectedBody = OptionalBody.body('{"something": 100, "somethingElse": 100}')
  }

  def 'matching json bodies - returns a mismatch - when the actual body has invalid value'() {
    expect:
    asJavaCollection(matcher.matchBody(expected(expectedBody), actual(actualBody), true)).find {
      it instanceof BodyMismatch &&
        it.mismatch.get().contains('Expected 100 but received 101')
    }

    where:

    actualBody = OptionalBody.body('{"something": 101}')
    expectedBody = OptionalBody.body('{"something": 100}')
  }

  def 'matching json bodies - returns a mismatch - when comparing a map to a list'() {
    expect:
    asJavaCollection(matcher.matchBody(expected(expectedBody), actual(actualBody), true)).find {
      it instanceof BodyMismatch &&
        it.mismatch.get().contains('Type mismatch: Expected Map Map(something -> 100, somethingElse -> 100) ' +
          'but received List List(100, 100)')
    }

    where:

    actualBody = OptionalBody.body('[100, 100]')
    expectedBody = OptionalBody.body('{"something": 100, "somethingElse": 100}')
  }

  def 'matching json bodies - returns a mismatch - when comparing list to anything'() {
    expect:
    asJavaCollection(matcher.matchBody(expected(expectedBody), actual(actualBody), true)).find {
      it instanceof BodyMismatch &&
        it.mismatch.get().contains('Type mismatch: Expected List List(100, 100) but received Integer 100')
    }

    where:

    actualBody = OptionalBody.body('100')
    expectedBody = OptionalBody.body('[100, 100]')
  }

  def 'matching json bodies - with a matcher defined - delegate to the matcher'() {
    given:
    matchers.addCategory('body').addRule('$.something', new RegexMatcher('\\d+'))

    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('{"something": 100}')
    expectedBody = OptionalBody.body('{"something": 101}')
  }

  def 'matching json bodies - with a matcher defined - and when the actual body is missing a key, not be a mismatch'() {
    given:
    matchers.addCategory('body').addRule('$.*', TypeMatcher.INSTANCE)

    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('{"something": 100, "other": 100}')
    expectedBody = OptionalBody.body('{"somethingElse": 100}')
  }

  def 'matching json bodies - with a matcher defined - defect 562: matching a list at the root with extra fields'() {
    given:
    matchers.addCategory('body').addRule('$', new MinTypeMatcher(1))
    matchers.addCategory('body').addRule('$[*].*', TypeMatcher.INSTANCE)

    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:

    actualBody = OptionalBody.body('''[
        {
            "documentId": 0,
            "documentCategoryId": 5,
            "documentCategoryCode": null,
            "contentLength": 0,
            "tags": null,
        },
        {
            "documentId": 1,
            "documentCategoryId": 5,
            "documentCategoryCode": null,
            "contentLength": 0,
            "tags": null,
        }
    ]''')
    expectedBody = OptionalBody.body('''[{
      "name": "Test",
      "documentId": 0,
      "documentCategoryId": 5,
      "contentLength": 0
    }]''')
  }
}
