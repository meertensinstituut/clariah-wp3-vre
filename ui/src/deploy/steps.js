import React from "react";
import {Pagination} from 'react-bootstrap';
import {withRouter} from 'react-router-dom';
import queryString from 'query-string';
import PropTypes from 'prop-types';
import StatePropsViewer from "../common/state-props-viewer";

/**
 * Keeps track of url params and different steps/states of deployment
 */
class Steps extends React.Component {

    constructor(props) {
        super(props);
        let params = normalizeParams(queryString.parse(this.props.location.search));
        let steps = this.props.steps;
        let completed = this.props.completed;
        let active = this.determineActiveStep(this.props.steps);

        let changedSteps = false;
        if (this.props.active !== active) {
            changedSteps = true;
        }
        if(this.addUrlValuesToSteps(params, steps)) {
            changedSteps = true;
        }

        this.state = {params, steps, active, completed, changedSteps};
    };

    static getDerivedStateFromProps(nextProps, prevState) {
        return {steps: nextProps.steps};
    }

    addUrlValuesToSteps(params, steps) {
        let changedSteps = false;
        Object.keys(params).forEach((key) => {
            let step = steps.find(s => s.key === key);
            if (step.value !== params[key]) {
                changedSteps = true;
                step.value = params[key];
            }
        });
        return changedSteps;
    }

    componentDidMount() {
        if (!this.state.changedSteps) {
            return;
        }
        this.props.onChangedSteps(
            this.state.steps,
            this.state.active,
            this.state.completed
        );
    }

    isDisabled(active, completed, i) {
        let disabled = true;
        if (i <= active) {
            disabled = false;
        } else if (i === active + 1) {
            disabled = !completed;
        }
        return disabled;
    }

    determineActiveStep(steps) {
        let indices = [];
        for (let i = 0; i < steps.length; i++) {
            if (steps[i].value !== null) indices.push(i);
        }
        let highestIndex = indices.indexOf(Math.max(...indices));
        return steps[highestIndex + 1].key;
    }

    goTo = (step) => {
        this.clearParamsAfterStep(step);
        updateUrlParams(convertStepsToParams(this.state.steps), this.props.history);
        this.props.onChangedSteps(this.state.steps, step.key, false);
    };

    clearParamsAfterStep(step) {
        let selected = this.state.steps.findIndex(s => s.key === step.key);
        this.state.steps.forEach((s, i) => {
            if (i >= selected) s.value = null;
        });
    }

    render() {
        let active = this.props.steps.findIndex((s) => s.key === this.props.active);
        return (
            <div>
                <Pagination>
                    {this.props.steps.map((step, i) => {
                        let disabled = this.isDisabled(active, this.props.completed, i);
                        return (
                            <Pagination.Item
                                key={i}
                                onClick={() => this.goTo(step)}
                                active={i === active}
                                disabled={disabled}
                            >
                                {step.label}
                            </Pagination.Item>
                        );
                    }, this)}
                </Pagination>
                <StatePropsViewer state={this.state} props={this.props} hide={true}/>
            </div>
        );
    }

}

function convertStepsToParams(steps) {
    let params = {};
    steps.forEach((s) => params[s.key] = s.value);
    return normalizeParams(params);
}

/**
 * Normalize parameters:
 * - Convert strings to booleans
 * - Remove params that have a value of null
 * - Sort params alphabetically by key
 */
function normalizeParams(params) {
    const result = {};
    const temp = Object.assign({}, params);
    Object.keys(params).forEach((key) => {
        temp[key] = stringToBoolean(params[key]);
        temp[key] = stringToNumber(temp[key]);
        if (temp[key] === null) delete temp[key];
    });
    Object
        .keys(temp)
        .sort()
        .forEach(key => result[key] = temp[key]);
    return result;
}

function stringToBoolean(val) {
    return val === 'true' || (val === 'false' ? false : val);
}

function stringToNumber(str) {
    return (/\d/.test(str) ? Number(str) : str);
}

function updateUrlParams(params, history) {
    history.push('/deploy?' + queryString.stringify(params));
}

Steps.propTypes = {
    steps: PropTypes.array.isRequired,
    active: PropTypes.string.isRequired,
    completed: PropTypes.bool.isRequired,
    onChangedSteps: PropTypes.func.isRequired
};

export default withRouter(Steps);