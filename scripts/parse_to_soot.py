# This script receives as input the path to a directory generated by the miningframework, it reads the output files and creates a [output]/data/results-soot.csv with the output in a format suported by a SOOT analysis framework

import sys
from csv import DictReader, writer

CLASS_NAME = "className"
LEFT_MODIFICATIONS = "left modifications"
RIGHT_MODIFICATIONS = "right modifications"
COMMIT_SHA = "merge commit"
PROJECT_NAME = "project"

output_path = sys.argv[1].rstrip("/") # get output path passed as cli argument
def export_csv():
    print ("Running parse to soot")
    scenarios = read_output(output_path)
    
    for scenario in scenarios:
        base_path = get_scenario_base_path(scenario)

        left_modifications = parse_modifications(scenario[LEFT_MODIFICATIONS])
        right_modifications = parse_modifications(scenario[RIGHT_MODIFICATIONS])
        class_name = scenario[CLASS_NAME]

        result = []
        result_reverse = []

        for line in left_modifications:
            if line not in right_modifications:
                result.append([class_name, "sink", line])
                result_reverse.append([class_name, "source", line])

        for line in right_modifications:
            if line not in left_modifications:
                result.append([class_name, "source", line])
                result_reverse.append([class_name, "sink", line])

        if result:
            with open(base_path + "/soot.csv", "w") as soot, open(base_path + "/soot-reverse.csv", "w") as soot_reverse:
                soot_writer = writer(soot, delimiter=",")
                soot_reverse_writer = writer(soot_reverse, delimiter=",")

                if result:
                    soot_writer.writerows(result)
                    soot_reverse_writer.writerows(result_reverse)

def read_output(output_path):
    with open(output_path + "/data/results-with-builds.csv", "r") as output_file:
        return list(DictReader(output_file, delimiter=";"))

def parse_modifications(modifications):
    trimmed_input = modifications.strip("[]").replace(" ", "")
    if (len (trimmed_input) > 0):
        return trimmed_input.split(",")
    return []

def get_scenario_base_path(scenario):
    return output_path + "/files/" + scenario[PROJECT_NAME] + "/" + scenario[COMMIT_SHA]

export_csv()