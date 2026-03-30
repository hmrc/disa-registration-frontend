#!/bin/bash

echo ""
echo "Applying migration LiaisonEmail"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /liaisonEmail                        controllers.LiaisonEmailController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /liaisonEmail                        controllers.LiaisonEmailController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeLiaisonEmail                  controllers.LiaisonEmailController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeLiaisonEmail                  controllers.LiaisonEmailController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "liaisonEmail.title = liaisonEmail" >> ../conf/messages.en
echo "liaisonEmail.heading = liaisonEmail" >> ../conf/messages.en
echo "liaisonEmail.checkYourAnswersLabel = liaisonEmail" >> ../conf/messages.en
echo "liaisonEmail.error.required = Enter liaisonEmail" >> ../conf/messages.en
echo "liaisonEmail.error.length = LiaisonEmail must be 100 characters or less" >> ../conf/messages.en
echo "liaisonEmail.change.hidden = LiaisonEmail" >> ../conf/messages.en

echo "Migration LiaisonEmail completed"
