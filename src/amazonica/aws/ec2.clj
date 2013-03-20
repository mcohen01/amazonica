(ns amazonica.aws.ec2
  "Amazon EC2 support."
  (:import com.amazonaws.services.ec2.AmazonEC2Client))

(amazonica.core/set-client AmazonEC2Client *ns*)