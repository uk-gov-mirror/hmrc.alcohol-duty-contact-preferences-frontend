/*
 * Copyright 2025 HM Revenue & Customs
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

package utils

import base.SpecBase
import models.CheckMode
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import play.api.i18n.Messages
import services.CountryService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._

class SummaryListHelperSpec extends SpecBase {

  implicit val messages: Messages = getMessages(app)

  val mockCountryService = mock[CountryService]

  val summaryListHelper = new SummaryListHelper(mockCountryService)

  val contactPreferenceRowEmail = SummaryListRowViewModel(
    key = KeyViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.key"))),
    value = ValueViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.email"))),
    actions = Seq(
      ActionItemViewModel(
        HtmlContent(messages("site.change")),
        controllers.changePreferences.routes.ContactPreferenceController.onPageLoad(CheckMode).url
      ).withVisuallyHiddenText(messages("checkYourAnswers.contactPreference.change.hidden"))
    )
  )

  val contactPreferenceRowPost = SummaryListRowViewModel(
    key = KeyViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.key"))),
    value = ValueViewModel(HtmlContent(messages("checkYourAnswers.contactPreference.post"))),
    actions = Seq(
      ActionItemViewModel(
        HtmlContent(messages("site.change")),
        controllers.changePreferences.routes.ContactPreferenceController.onPageLoad(CheckMode).url
      ).withVisuallyHiddenText(messages("checkYourAnswers.contactPreference.change.hidden"))
    )
  )

  val emailAddressRow = SummaryListRowViewModel(
    key = KeyViewModel(HtmlContent(messages("checkYourAnswers.emailAddress.key"))),
    value = ValueViewModel(HtmlContent(emailAddress)),
    actions = Seq(
      ActionItemViewModel(
        HtmlContent(messages("site.change")),
        controllers.changePreferences.routes.EnterEmailAddressController.onPageLoad(CheckMode).url
      ).withVisuallyHiddenText(messages("checkYourAnswers.emailAddress.change.hidden"))
    )
  )

  val correspondenceAddressWithCountry = correspondenceAddress + "\nUnited Kingdom"

  val correspondenceAddressHtmlString = "<span class='break'>Flat 123</span><span class='break'>1 Example Road</span><span class='break'>London</span><span class='break'>AB1 2CD</span><span class='break'>United Kingdom</span>"

  val correspondenceAddressRow = SummaryListRowViewModel(
    key = KeyViewModel(HtmlContent(messages("checkYourAnswers.correspondenceAddress.key"))),
    value = ValueViewModel(HtmlContent(correspondenceAddressHtmlString))
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCountryService)
  }

  "checkYourAnswersSummaryList" - {
    "must return a summary list with the correct rows if email is selected" in {
      val summaryList = summaryListHelper.checkYourAnswersSummaryList(userAnswersPostNoEmail)

      summaryList mustBe SummaryListViewModel(rows = Seq(contactPreferenceRowEmail, emailAddressRow))
      verify(mockCountryService, times(0)).tryLookupCountryName(any())
    }

    "must return a summary list with the correct rows if post is selected" in {
      when(mockCountryService.tryLookupCountryName(any())) thenReturn Some("United Kingdom")

      val summaryList = summaryListHelper.checkYourAnswersSummaryList(userAnswers)

      summaryList mustBe SummaryListViewModel(rows = Seq(contactPreferenceRowPost, correspondenceAddressRow))
      verify(mockCountryService, times(1)).tryLookupCountryName(countryCode)
    }

    "must throw an exception if no contact preference is selected" in {
      val exception = intercept[IllegalStateException] {
        summaryListHelper.checkYourAnswersSummaryList(emptyUserAnswers)
      }
      exception.getMessage mustBe "User answers do not contain the required data but not picked up by PageCheckHelper"
    }

    "must throw an exception if email is selected but no email address is provided" in {
      val exception = intercept[IllegalStateException] {
        summaryListHelper.checkYourAnswersSummaryList(userAnswersPostWithEmail.copy(emailAddress = None))
      }
      exception.getMessage mustBe "User answers do not contain the required data but not picked up by PageCheckHelper"
    }
  }

  "correspondenceAddressSummaryList" - {
    "must return a summary list with the correspondence address row" in {
      when(mockCountryService.tryLookupCountryName(any())) thenReturn Some("United Kingdom")

      val summaryList = summaryListHelper.correspondenceAddressSummaryList(userAnswers)

      summaryList mustBe SummaryListViewModel(rows = Seq(correspondenceAddressRow))
      verify(mockCountryService, times(1)).tryLookupCountryName(countryCode)
    }
  }

  "getFullCorrespondenceAddress" - {
    "must return an address with a country if the CountryService is able to look up the country name" in {
      when(mockCountryService.tryLookupCountryName(any())) thenReturn Some("United Kingdom")

      summaryListHelper.getFullCorrespondenceAddress(subscriptionSummaryEmail) mustBe correspondenceAddressWithCountry
      verify(mockCountryService, times(1)).tryLookupCountryName(eqTo(countryCode))
    }

    "must return an address with no country if the CountryService is unable to look up the country name" in {
      when(mockCountryService.tryLookupCountryName(any())) thenReturn None

      summaryListHelper.getFullCorrespondenceAddress(subscriptionSummaryEmail) mustBe correspondenceAddress
      verify(mockCountryService, times(1)).tryLookupCountryName(eqTo(countryCode))
    }

    "must return an address with no country if country code is missing from the subscription summary" in {
      summaryListHelper.getFullCorrespondenceAddress(
        subscriptionSummaryEmail.copy(countryCode = None)
      ) mustBe correspondenceAddress

      verify(mockCountryService, times(0)).tryLookupCountryName(any())
    }
  }
}
