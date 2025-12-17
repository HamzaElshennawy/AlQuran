[//]: # (- # Table of Contents)

[//]: # ()

[//]: # (    - [Ways to Contribute]&#40;#ways-to-contribute&#41;)

[//]: # (        - [Code Contributions]&#40;#code-contributions&#41;)

[//]: # (        - [Translations]&#40;#translations&#41;)

[//]: # (        - [Documentation]&#40;#documentation&#41;)

[//]: # (        - [Testing]&#40;#testing&#41;)

[//]: # (    - [Development Workflow]&#40;#development-workflow&#41;)

[//]: # (        - [Important]&#40;#important&#41;)

[//]: # (            - [The Actions secrets and variables are]&#40;#the-actions-secrets-and-variables-are&#41;)

[//]: # (            - [Create Actions secrets and variables]&#40;#create-actions-secrets-and-variables&#41;)

[//]: # (                - [create a signing key]&#40;#create-a-signing-key&#41;)

[//]: # (                - [convert the signing key to a base64 string]&#40;#convert-the-signing-key-to-a-base64-string&#41;)

[//]: # (                - [go to repo Settings > Secrets and Variables > Actions]&#40;#go-to-repo-settings--secrets-and-variables--actions&#41;)

[//]: # (                - [create 3 New repository secrets]&#40;#create-3-new-repository-secrets-as-follows&#41;)

[//]: # (        - [Fork the repository]&#40;#fork-the-repository&#41;)

[//]: # (        - [Create a feature branch]&#40;#create-a-feature-branch&#41;)

[//]: # (        - [Commit your changes]&#40;#commit-your-changes&#41;)

[//]: # (        - [Push to the branch]&#40;#push-to-the-branch&#41;)

[//]: # (        - [Open a Pull Request]&#40;#open-a-pull-request&#41;)

[//]: # (        - [Code Style]&#40;#code-style&#41;)

- # Ways to Contribute

    - ## Code Contributions

        - Fix bugs
        - Add new features
        - Improve performance
        - Enhance UI/UX

    - ## Translations

        - Translate the app via [Crowdin](https://crowdin.com/project/alquran)

    - ## Documentation

        - Improve README and wiki
        - Write tutorials
        - Create video guides

    - ## Testing

        - Report bugs via [GitHub Issues](https://github.com/abdalmoniem/AlQuran/issues)
        - Test new features
        - Verify on different devices

- # Development Workflow

    - ### Important

      This project uses `GitHub Actions Workflows` with `Actions secrets and variables` and
      `branch rulesets` with `status checks` to validate new commits and PRs, you ***MUST*** create these
      `Actions secrets and variables` in your forked repo settings for the `status checks` to pass as the
      workflows will run in the context of your forked repo! please follow the following steps to create
      these `Actions secrets and variables`:

        - #### The `Actions secrets and variables` are

            - **SIGNING_KEY**
            - **KEY_PASSWORD**
            - **KEY_STORE_PASSWORD**

          These are `signing key` secrets to be used to sign the `build variants` of the App,
          ***THESE SHOULD NOT BE SHARED WITH ANYONE***

        -  #### Create `Actions secrets and variables`

            - ##### create a `signing key`:
              ```bash
              keytool -genkey -v \
              -keystore `path to release_keystore.jks` \
              -alias release_key -storetype JKS -keyalg RSA -validity 10950
              ```

              you'll be prompted to enter some information about the keystore:
              ```txt
              Enter keystore password:
              Re-enter new password:
              Enter the distinguished name. Provide a single dot (.)
              to leave a sub-component empty or press ENTER to use the
              default value in braces.
              What is your first and last name?
              [Unknown]:  First Name Last Name
              What is the name of your organizational unit?
              [Unknown]:
              What is the name of your organization?
              [Unknown]:
              What is the name of your City or Locality?
              [Unknown]:
              What is the name of your State or Province?
              [Unknown]:
              What is the two-letter country code for this unit?
              [Unknown]:
              Is CN=First Name Last Name, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown,
              C=Unknown correct?
              [no]:  yes
              
              Generating 3,072 bit RSA key pair and self-signed certificate (SHA384withRSA) with a
              validity of 10,950 days
              for: CN=First Name Last Name, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown,
              C=Unknown
              Enter key password for <release_key>
              (RETURN if same as keystore password):
              Re-enter new password:
              [Storing keystore.jks]
              
              Warning:
              The JKS keystore uses a proprietary format. It is recommended to migrate to
              PKCS12 which is an industry standard format using "keytool -importkeystore
              -srckeystore keystore.jks -destkeystore keystore.jks -deststoretype pkcs12".
              ```

              > take note of the `keystore password` and the `key password` you entered

            - ##### convert the `signing key` to a `base64 string`:
              ```bash
              echo $(base64 < `path to release_keystore.jks` | tr -d '\n')
              ```
              > take note of the `base64 string` generated

            - ##### go to repo `Settings` > `Secrets and Variables` > `Actions`

            - ##### create 3 `New repository secrets` as follows:

                - Name: **SIGNING_KEY**

                  Secret: `base64 string` from the
                  [base64 string](#convert-the-signing-key-to-a-base64-string) step

                - Name: **KEY_PASSWORD**

                  Secret: `key password` from the [signing key](#create-a-signing-key) step

                - Name: **KEY_STORE_PASSWORD**

                  Secret: `keystore password` from the [signing key](#create-a-signing-key) step

    - ### Fork the repository

    - ### Create a feature branch
      ```bash
      git checkout -b feature/NewFeatureX
      ```

    - ### Commit your changes
      ```bash
      git commit -m 'Added New Feature X'
      ```

    - ### Push to the branch
      ```bash
      git push origin feature/NewFeatureX
      ```

    - ### Open a Pull Request

    - ### Code Style

        - Follow Kotlin coding conventions
        - Use meaningful variable and function names
        - Add comments for complex logic
        - Write unit tests for new features