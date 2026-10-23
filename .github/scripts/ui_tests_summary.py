#!/usr/bin/env python3
"""Step summary of a browser tests run (ui-tests.yml) from the failsafe reports: the totals, the stack's start, the time
a test takes, and the failed and flaky tests with what they failed with."""
import glob
import os
import xml.etree.ElementTree as ET

REPORTS = os.path.join(os.path.dirname(__file__), "..", "..", "tests", "target", "failsafe-reports")


def name(case):
    return "`%s.%s`" % ((case.get("classname") or "").rsplit(".", 1)[-1], case.get("name"))


# the test's first problem with one of the tags, on one line: playwright's messages start with "Error {"
def problem(case, tags):
    for element in case:
        if element.tag in tags:
            message = " ".join((element.get("message") or element.get("type") or "").replace("`", "'").split())
            return "- %s: `%s`" % (name(case), message[:300] + "..." if len(message) > 300 else message)


skipped = 0
seconds = start = 0.0
times, failed, flaky = [], [], []
for report in sorted(glob.glob(os.path.join(REPORTS, "TEST-*.xml"))):
    suite = ET.parse(report).getroot()
    cases = list(suite.iter("testcase"))
    seconds += float(suite.get("time", 0))
    # the stack starts in the @BeforeClass of the run's first class, outside any test's time
    start = max(start, float(suite.get("time", 0)) - sum(float(case.get("time", 0)) for case in cases))
    for case in cases:
        times.append((float(case.get("time", 0)), name(case)))
        line = problem(case, ("failure", "error"))
        if line:
            failed.append(line)
        else:
            line = problem(case, ("flakyFailure", "flakyError"))
            if line:
                flaky.append(line)
            elif case.find("skipped") is not None:
                skipped += 1

if not times:
    print("No test reports: the run stopped before the tests, see the log")
else:
    times.sort()
    print("**%d tests: %d failed, %d passed when rerun, %d skipped**\n" % (len(times), len(failed), len(flaky), skipped))
    print("%.1f min in the test classes, %.0f s of it the stack's start. A test takes %.1f s on average, %.1f s the "
          "median, %.1f s the 90th percentile, %.1f s the longest, %s\n"
          % (seconds / 60, start, sum(time for time, _ in times) / len(times), times[len(times) // 2][0],
             times[len(times) * 9 // 10][0], *times[-1]))
    for title, lines in (("Failed", failed), ("Passed when rerun", flaky)):
        if lines:
            print("%s:\n%s\n" % (title, "\n".join(lines)))
